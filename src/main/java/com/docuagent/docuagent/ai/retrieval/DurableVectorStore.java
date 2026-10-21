package com.docuagent.docuagent.ai.retrieval;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/** 文件型向量库的串行读写及原子落盘，防止文件被写到一半。 */
public class DurableVectorStore extends SimpleVectorStore {
    public DurableVectorStore(EmbeddingModel model) { super(SimpleVectorStore.builder(model)); }
    @Override public synchronized void doAdd(List<Document> documents) { super.doAdd(documents); }
    @Override public synchronized void doDelete(List<String> ids) { super.doDelete(ids); }
    @Override public synchronized void doDelete(Filter.Expression filter) { super.doDelete(filter); }
    @Override public synchronized List<Document> doSimilaritySearch(SearchRequest request) { return super.doSimilaritySearch(request); }

    @Override
    public synchronized void save(File file) {
        Path target = file.toPath().toAbsolutePath().normalize(), temporary = null;
        try {
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), "vectors-", ".tmp");
            super.save(temporary.toFile());
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            throw new IllegalStateException("向量索引保存失败", exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }
}
