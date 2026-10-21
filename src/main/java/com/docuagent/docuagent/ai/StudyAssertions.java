package com.docuagent.docuagent.ai;

import com.docuagent.docuagent.vo.RetrievalHitVO;
import java.util.*;

/** 从已确认来源的课程陈述建立二选一判断题素材，不允许模型改写原文事实。 */
public final class StudyAssertions {
    public record Assertion(Long chunkId,String text) { }
    private StudyAssertions() { }
    public static Map<String,Assertion> from(List<RetrievalHitVO> sources){
        Map<String,Assertion> result=new LinkedHashMap<>();Set<String> seen=new HashSet<>();
        for(var source:sources)for(String line:source.content().split("\\R")){
            String original=line.trim();
            if(!original.matches("^[•Ø➢].*"))continue;
            String text=original.replaceFirst("^[•Ø➢\\s]+","").trim();
            if(text.length()<10||text.length()>160||text.matches(".*[，、：:（(？?]$")||text.matches(".*(例如|比如|练习|思考|假设).*"))continue;
            // 脱离标题就缺少主语的动作列表不能直接成为独立判断题。
            if(text.matches("^(读入|过滤|将|把|从|向|用|交换|删除|插入|添加|输出|生成|构造|选择|识别|它|其|可以|能够|描述|表示).*"))continue;
            if(!text.matches(".*(是|为|有|将|把|由|可以|读入|过滤|添加|表示|用于|包含|组成|生成|构造|支持|选择|识别).*"))continue;
            if(seen.add(text))result.put("A"+result.size(),new Assertion(source.chunkId(),text));
            if(result.size()>=40)return result;
        }
        return result;
    }
}
