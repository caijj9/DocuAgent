package com.docuagent.docuagent.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class VectorStoreConfig {

    @Value("${docuagent.vector-store.path:data/vector-store.json}")
    private String storePath;

    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        // builder 需要一个 EmbeddingModel —— Spring AI 会按配置自动注入（硅基流动那个）
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        // 启动时把上次落盘的向量读回来。文件不存在就跳过（第一次跑必然不存在）
        File file = new File(storePath);
        if (file.exists()) {
            store.load(file);
        }
        return store;
    }
}
