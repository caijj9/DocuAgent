package com.docuagent.docuagent.retrieval;

import com.docuagent.docuagent.ai.retrieval.BatchReranker;
import com.docuagent.docuagent.exception.BizException;
import com.docuagent.docuagent.mapper.ChunkMapper;
import com.docuagent.docuagent.mapper.CourseMapper;
import com.docuagent.docuagent.service.impl.RetrievalServiceImpl;
import com.docuagent.docuagent.vo.RetrievalCandidate;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class RetrievalServiceTest {
    private final SimpleVectorStore vectors=mock(SimpleVectorStore.class);
    private final ChunkMapper chunks=mock(ChunkMapper.class);
    private final CourseMapper courses=mock(CourseMapper.class);
    private final BatchReranker reranker=mock(BatchReranker.class);
    private final RetrievalServiceImpl service=new RetrievalServiceImpl(vectors,chunks,courses,reranker,5,0,60,10);

    private org.springframework.ai.document.Document vector(long id, long course, String text) {
        return org.springframework.ai.document.Document.builder().id(String.valueOf(id)).text(text)
                .metadata(Map.of("chunkId",id,"documentId",10L,"courseId",course)).score(0.9).build();
    }

    @Test void invalidAndEmptyCourseRequestsShouldNotCallModels() {
        assertThatThrownBy(() -> service.search("",1L,"vector",false,false,null)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.search("问题",1L,"unknown",false,false,null)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.search("问题",1L,"both",false,true,null)).isInstanceOf(BizException.class);
        when(courses.selectCount(any())).thenReturn(1L);
        when(chunks.selectRetrievable(2L,20001)).thenReturn(List.of());
        assertThat(service.search("问题",2L,"both",true,true,null)).isEmpty();
        verifyNoInteractions(vectors,reranker);
    }

    @Test void orphanWrongCourseAndStaleTextShouldNotBecomeReferences() {
        when(courses.selectCount(any())).thenReturn(1L);
        when(chunks.selectRetrievable(1L,20001)).thenReturn(List.of(new RetrievalCandidate(1L,10L,1L,"词法分析","真实正文")));
        when(vectors.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(vector(99,1,"孤儿"),vector(1,2,"真实正文"),vector(1,1,"旧正文"),vector(1,1,"真实正文")));
        assertThat(service.search("问题",1L,"vector",false,false,null)).hasSize(1).first()
                .extracting(h -> h.chunkId()).isEqualTo(1L);
    }

    @Test void keywordOnlyShouldNotCallEmbeddingOrChat() {
        when(courses.selectCount(any())).thenReturn(1L);
        when(chunks.selectRetrievable(1L,20001)).thenReturn(List.of(new RetrievalCandidate(1L,10L,1L,"词法分析","读入字符流，输出词法单元")));
        assertThat(service.search("词法",1L,"keyword",false,false,null)).hasSize(1);
        verifyNoInteractions(vectors,reranker);
    }
}
