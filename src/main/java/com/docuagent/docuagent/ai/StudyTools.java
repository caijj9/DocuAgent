package com.docuagent.docuagent.ai;

import com.docuagent.docuagent.common.CodeEnum;
import com.docuagent.docuagent.exception.BizException;
import com.docuagent.docuagent.service.*;
import com.docuagent.docuagent.vo.StudyViews.ToolTrace;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import java.util.*;

/** 每次请求创建独立工具实例，课程范围由服务器绑定，模型不能改成另一门课。 */
@Slf4j
public class StudyTools {
    private final Long courseId; private final boolean maySave; private final LearningService learning;
    private final RetrievalService retrieval; private final ObjectMapper mapper; private final List<ToolTrace> traces=new ArrayList<>();
    public StudyTools(Long courseId,boolean maySave,LearningService learning,RetrievalService retrieval,ObjectMapper mapper){
        this.courseId=courseId;this.maySave=maySave;this.learning=learning;this.retrieval=retrieval;this.mapper=mapper;
    }
    @Tool(description="用户要求解释课程资料、核对知识点时使用。只搜索当前绑定课程的课件，返回分块原文及来源。")
    public String searchCourseMaterial(@ToolParam(description="要查找的完整问题或关键词") String query){
        return invoke("searchCourseMaterial",parameters("query",query),()->{
            requireScope();return retrieval.search(query,courseId,"both",true,false,5);
        });
    }
    @Tool(description="用户问我上次错在哪里、最近答题或错题情况时使用，必须先读取真实记录再回答，不能凭空编造。")
    public String readAnswerRecords(@ToolParam(description="最近多少天，1到365，通常30") int days){
        return invoke("readAnswerRecords",parameters("days",days),()->{requireScope();return learning.history(courseId,days);});
    }
    @Tool(description="仅当用户明确要求收藏、记下或加入错题本时使用；指定已有题目ID。手动收藏不会增加答错次数。")
    public String saveWrongQuestion(@ToolParam(description="用户明确指定或对话中已确定的题目ID，不得编造") Long questionId){
        return invoke("saveWrongQuestion",parameters("questionId",questionId),()->{
            requireScope();if(!maySave)throw new BizException(CodeEnum.TOOL_ACTION_NOT_ALLOWED);
            return learning.saveManual(courseId,questionId);
        });
    }
    public List<ToolTrace> traces(){return List.copyOf(traces);}
    private String parameters(String name,Object value){
        try{return mapper.writeValueAsString(Collections.singletonMap(name,value));}
        catch(Exception exception){throw new BizException(CodeEnum.MODEL_OUTPUT_INVALID);}
    }
    private void requireScope(){if(courseId==null)throw new BizException(CodeEnum.TOOL_ACTION_NOT_ALLOWED);}
    private synchronized String invoke(String name,String parameters,java.util.function.Supplier<Object> action){
        if(traces.size()>=4)throw new BizException(CodeEnum.MODEL_OUTPUT_INVALID);
        String result;
        try{result=mapper.writeValueAsString(Map.of("success",true,"data",action.get()));}
        catch(BizException exception){result="{\"success\":false,\"code\":"+exception.getCode()+",\"message\":\"工具条件不满足\"}";}
        catch(Exception exception){result="{\"success\":false,\"message\":\"工具执行失败\"}";}
        traces.add(new ToolTrace(name,parameters,result));
        log.info("工具执行：name={} courseId={} parameters={} result={}",name,courseId,parameters,result);
        return result;
    }
}
