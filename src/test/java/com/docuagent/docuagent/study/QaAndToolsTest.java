package com.docuagent.docuagent.study;

import com.docuagent.docuagent.ai.*;
import com.docuagent.docuagent.dto.StudyRequests.Ask;
import com.docuagent.docuagent.service.*;
import com.docuagent.docuagent.service.impl.QaServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class QaAndToolsTest {
    @Test void emptyRetrievalShouldRefuseWithoutCallingChat(){
        RetrievalService retrieval=mock(RetrievalService.class);StudyModel model=mock(StudyModel.class);
        SessionService sessions=mock(SessionService.class);LearningService learning=mock(LearningService.class);
        var context=new SessionService.Context(null,2L,List.of(),null);
        when(sessions.open(null,2L)).thenReturn(context);
        when(retrieval.search(anyString(),eq(2L),eq("both"),eq(true),eq(false),eq(10))).thenReturn(List.of());
        var service=new QaServiceImpl(retrieval,model,sessions,learning,new ObjectMapper(),mock(ChatModel.class));
        var answer=service.ask(new Ask("问题",2L,null));assertThat(answer.references()).isEmpty();
        assertThat(answer.answer()).contains("资料中没有相关内容");verifyNoInteractions(model);verify(sessions).complete(context,"问题",answer.answer());
    }
    @Test void modelCannotSaveWithoutExplicitUserRequestOrBoundCourse(){
        LearningService learning=mock(LearningService.class);RetrievalService retrieval=mock(RetrievalService.class);
        var tools=new StudyTools(1L,false,learning,retrieval,new ObjectMapper());
        assertThat(tools.saveWrongQuestion(1L)).contains("\"success\":false");verifyNoInteractions(learning);
        var unscoped=new StudyTools(null,true,learning,retrieval,new ObjectMapper());
        assertThat(unscoped.saveWrongQuestion(1L)).contains("\"success\":false");verifyNoInteractions(learning);
    }
    @Test void resolvingAPronounMustPreserveTheRestOfTheQuestionVerbatim() throws Exception {
        RetrievalService retrieval=mock(RetrievalService.class);StudyModel model=mock(StudyModel.class);
        SessionService sessions=mock(SessionService.class);var mapper=new ObjectMapper();
        var context=new SessionService.Context("test-session",1L,List.of(new SessionService.Turn("词法分析器负责什么？","读入字符流，输出词法单元。")),"token");
        when(sessions.open("test-session",null)).thenReturn(context);
        when(model.json(anyString(),any(),anyInt())).thenReturn(mapper.readTree("{\"R0\":\"词法分析器\"}"));
        when(retrieval.search(anyString(),eq(1L),eq("both"),eq(true),eq(false),eq(10))).thenReturn(List.of());
        var service=new QaServiceImpl(retrieval,model,sessions,mock(LearningService.class),mapper,mock(ChatModel.class));
        var result=service.ask(new Ask("上面那个组件会把词素放到哪里？",null,"test-session"));
        assertThat(result.rewrittenQuestion()).isEqualTo("词法分析器会把词素放到哪里？");
    }
}
