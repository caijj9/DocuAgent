package com.docuagent.docuagent.ai;

import com.docuagent.docuagent.entity.Question;
import com.docuagent.docuagent.vo.RetrievalHitVO;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Component;
import java.math.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** 模型输出先校验后使用，所有题目校验完成前不允许写数据库。 */
@Component
public class StudyOutputValidator {
    private final ObjectMapper mapper;
    public StudyOutputValidator(ObjectMapper mapper){this.mapper=mapper;}
    public record Grade(BigDecimal score,boolean correct,String comment,List<String> hit,List<String> missed) { }

    public List<Question> questions(JsonNode root,Long courseId,int count,String requestedType,List<RetrievalHitVO> sources) throws Exception {
        if(!root.isArray()||root.size()!=count)throw new IllegalArgumentException("题量不匹配");
        Map<Long,String> sourceTexts=new HashMap<>();
        for(var source:sources)sourceTexts.put(source.chunkId(),source.content());
        List<Question> result=new ArrayList<>(); Set<String> stems=new HashSet<>(); Set<String> types=new HashSet<>();
        Map<String,StudyAssertions.Assertion> assertions=StudyAssertions.from(sources);Set<String> selected=new HashSet<>();
        for(JsonNode item:root){
            String requested=text(item,"type",20);
            if(requested.equals("single_choice")&&item.has("assertionId")){
                if(!requestedType.equals("mixed")&&!requestedType.equals("single_choice"))throw new IllegalArgumentException("题型不匹配");
                String id=text(item,"assertionId",20);var assertion=assertions.get(id);
                if(assertion==null||!selected.add(id))throw new IllegalArgumentException("事实来源不存在或重复");
                boolean positive=ThreadLocalRandom.current().nextBoolean(),correctFirst=ThreadLocalRandom.current().nextBoolean();
                String correct=positive?"正确":"错误",incorrect=positive?"错误":"正确";
                Question question=new Question();question.setCourseId(courseId);question.setType("single_choice");
                question.setStem("根据课件判断："+(positive?"“"+assertion.text()+"”":"并非“"+assertion.text()+"”")+"。该说法是否正确？");
                question.setOptions(mapper.writeValueAsString(correctFirst?List.of("A. "+correct,"B. "+incorrect):List.of("A. "+incorrect,"B. "+correct)));
                question.setAnswer(correctFirst?"A":"B");question.setKeyPoints(mapper.writeValueAsString(List.of(assertion.text())));
                question.setSourceChunkId(assertion.chunkId());question.setEvidenceChoice(true);result.add(question);types.add("single_choice");continue;
            }
            String stem=text(item,"stem",2000),type=text(item,"type",20),answer=text(item,"answer",500);
            if(!Set.of("single_choice","short_answer").contains(type)||(!requestedType.equals("mixed")&&!requestedType.equals(type))||!stems.add(stem))
                throw new IllegalArgumentException("题型或题干无效");
            List<String> points=strings(item.get("keyPoints"),1,8,300);
            if(type.equals("short_answer")&&points.size()<2)throw new IllegalArgumentException("简答题至少两个评分要点");
            JsonNode source=item.get("sourceChunkId");
            if(source==null||!source.isIntegralNumber()||!source.canConvertToLong()||!sourceTexts.containsKey(source.longValue()))
                throw new IllegalArgumentException("来源不在本次检索结果中");
            String options=null;
            if(type.equals("single_choice")){
                List<String> choices=new ArrayList<>(strings(item.get("options"),2,6,300));
                // 接受纯文本选项，由服务器确定性编号；若模型已写标签，必须按顺序，不能悄悄重排答案。
                for(int i=0;i<choices.size();i++){
                    String choice=choices.get(i),label=((char)('A'+i))+". ";
                    if(choice.matches("^[A-F][.、)].*")){
                        if(!choice.startsWith(label))throw new IllegalArgumentException("选项标签必须连续");
                    }else choices.set(i,label+choice);
                }
                if(!answer.matches("[A-F]")||answer.charAt(0)-'A'>=choices.size())throw new IllegalArgumentException("答案不是有效选项标签");
                options=mapper.writeValueAsString(choices);
                if(options.length()>1000)throw new IllegalArgumentException("选项过长");
            }
            String pointJson=mapper.writeValueAsString(points);
            if(pointJson.length()>1000)throw new IllegalArgumentException("评分要点过长");
            Question question=new Question(); question.setCourseId(courseId);question.setType(type);question.setStem(stem);
            question.setOptions(options);question.setAnswer(answer);question.setKeyPoints(pointJson);question.setSourceChunkId(source.longValue());
            result.add(question);types.add(type);
        }
        if(requestedType.equals("mixed")&&count>1&&types.size()!=2)throw new IllegalArgumentException("mixed须包含两种题型");
        return result;
    }

    /** 评分要点必须逐项出现一次；分数与正确性由服务器计算，不接受模型任意分值。 */
    public Grade grade(JsonNode root,List<String> points,String userAnswer){
        JsonNode values=root.get("points");
        if(values==null||!values.isObject()||values.size()!=points.size())throw new IllegalArgumentException("评分要点数量错误");
        List<String> hit=new ArrayList<>(),missed=new ArrayList<>();
        for(int i=0;i<points.size();i++){
            JsonNode value=values.get("P"+i);
            if(value==null||!value.isTextual())throw new IllegalArgumentException("要点必须给出用户答案原文证据或空串");
            String quote=value.textValue().trim();
            if(!quote.isEmpty()&&!userAnswer.replaceAll("\\s+","").contains(quote.replaceAll("\\s+","")))
                throw new IllegalArgumentException("命中证据不在用户答案中");
            (quote.isEmpty()?missed:hit).add(points.get(i));
        }
        BigDecimal score=BigDecimal.TEN.multiply(BigDecimal.valueOf(hit.size())).divide(BigDecimal.valueOf(points.size()),2,RoundingMode.HALF_UP);
        return new Grade(score,missed.isEmpty(),text(root,"comment",1000),hit,missed);
    }

    public static String text(JsonNode node,String name,int max){
        JsonNode value=node.get(name);
        if(value==null||!value.isTextual()||value.textValue().isBlank()||value.textValue().length()>max)throw new IllegalArgumentException("字段无效："+name);
        return value.textValue().trim();
    }

    public static List<String> strings(JsonNode node,int min,int max,int length){
        if(node==null||!node.isArray()||node.size()<min||node.size()>max)throw new IllegalArgumentException("数组数量错误");
        List<String> result=new ArrayList<>();
        for(JsonNode value:node){
            if(!value.isTextual()||value.textValue().isBlank()||value.textValue().length()>length||result.contains(value.textValue().trim()))throw new IllegalArgumentException("数组内容错误");
            result.add(value.textValue().trim());
        }
        return result;
    }
}
