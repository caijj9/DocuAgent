package com.docuagent.docuagent.retrieval;

import com.docuagent.docuagent.ai.chunk.SimpleChunker;
import com.docuagent.docuagent.entity.Chunk;
import com.docuagent.docuagent.entity.Course;
import com.docuagent.docuagent.entity.Document;
import com.docuagent.docuagent.exception.BizException;
import com.docuagent.docuagent.mapper.ChunkMapper;
import com.docuagent.docuagent.mapper.CourseMapper;
import com.docuagent.docuagent.mapper.DocumentMapper;
import com.docuagent.docuagent.service.impl.DocumentServiceImpl;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class UploadIndexFailureTest {
    @TempDir Path temporary;

    @Test void secondEmbeddingBatchFailureShouldMarkDocumentFailedAndCleanPartialVectors() throws Exception {
        var service = new DocumentServiceImpl();
        DocumentMapper documents=mock(DocumentMapper.class);
        ChunkMapper chunks=mock(ChunkMapper.class);
        CourseMapper courses=mock(CourseMapper.class);
        SimpleChunker chunker=mock(SimpleChunker.class);
        SimpleVectorStore vectors=mock(SimpleVectorStore.class);
        ReflectionTestUtils.setField(service,"documentMapper",documents);
        ReflectionTestUtils.setField(service,"chunkMapper",chunks);
        ReflectionTestUtils.setField(service,"courseMapper",courses);
        ReflectionTestUtils.setField(service,"simpleChunker",chunker);
        ReflectionTestUtils.setField(service,"vectorStore",vectors);
        ReflectionTestUtils.setField(service,"storePath",temporary.resolve("index.json").toString());
        when(courses.selectById(1L)).thenReturn(new Course());
        when(documents.selectCount(any())).thenReturn(0L);
        when(documents.insert(any(Document.class))).thenAnswer(invocation->{((Document)invocation.getArgument(0)).setId(10L);return 1;});
        AtomicLong nextId=new AtomicLong(100);
        when(chunks.insert(any(Chunk.class))).thenAnswer(invocation->{((Chunk)invocation.getArgument(0)).setId(nextId.incrementAndGet());return 1;});
        when(chunker.split(anyString())).thenReturn(Collections.nCopies(11,"课程原文"));
        doNothing().doThrow(new IllegalStateException("simulated provider failure")).when(vectors).add(anyList());
        String oldDirectory=System.getProperty("user.dir");
        try {
            System.setProperty("user.dir",temporary.toString());
            var file=new MockMultipartFile("file","lesson.doc","application/msword","A course note for a deterministic indexing regression.".getBytes(StandardCharsets.UTF_8));
            assertThatThrownBy(()->service.uploadDocument(1L,file)).isInstanceOf(BizException.class);
        } finally { System.setProperty("user.dir",oldDirectory); }
        verify(vectors).delete(argThat((java.util.List<String> ids)->ids.size()==11));
        verify(documents,atLeastOnce()).updateById(argThat((Document document)->"FAILED".equals(document.getStatus())));
        verify(vectors).save(any(java.io.File.class));
    }
}
