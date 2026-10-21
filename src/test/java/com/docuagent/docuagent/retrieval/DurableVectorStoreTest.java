package com.docuagent.docuagent.retrieval;

import com.docuagent.docuagent.ai.retrieval.DurableVectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class DurableVectorStoreTest {
    @TempDir Path temporary;
    @Test void restartAndDeleteShouldPersistAndRespectCourseFilter() {
        EmbeddingModel embedding=mock(EmbeddingModel.class);
        when(embedding.embed(any(Document.class))).thenReturn(new float[]{1,0,0});
        when(embedding.embed(anyString())).thenReturn(new float[]{1,0,0});
        var first=new DurableVectorStore(embedding);
        first.add(List.of(new Document("1","正文",Map.of("courseId",1))));
        Path file=temporary.resolve("nested/index.json");
        first.save(file.toFile());
        var second=new DurableVectorStore(embedding);
        second.load(file.toFile());
        assertThat(second.similaritySearch(SearchRequest.builder().query("正文").filterExpression("courseId == 1").build())).hasSize(1);
        assertThat(second.similaritySearch(SearchRequest.builder().query("正文").filterExpression("courseId == 2").build())).isEmpty();
        second.delete(List.of("1")); second.save(file.toFile());
        var third=new DurableVectorStore(embedding); third.load(file.toFile());
        assertThat(third.similaritySearch("正文")).isEmpty();
    }
}
