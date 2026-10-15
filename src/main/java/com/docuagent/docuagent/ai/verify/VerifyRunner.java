package com.docuagent.docuagent.ai.verify;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Day 0 技术验证入口：跑通「聊天 / Embedding / 工具调用」三个最小示例。
 * <p>
 * 用 {@code @Profile("verify")} 控制 —— 只有激活 verify profile 时才执行，
 * 平时正常启动不会跑（避免每次启动都调用模型、重复计费）。
 * <p>
 * 运行方式：Run Configuration 的 Active profiles 填 {@code verify}。
 */
@Component
@Profile("verify")
public class VerifyRunner implements CommandLineRunner {

    private final ChatClient.Builder chatClientBuilder;
    private final EmbeddingModel embeddingModel;

    /**
     * 构造器注入：ChatClient.Builder 与 EmbeddingModel 均由 Spring AI 自动配置提供。
     */
    public VerifyRunner(ChatClient.Builder chatClientBuilder, EmbeddingModel embeddingModel) {
        this.chatClientBuilder = chatClientBuilder;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n========== Day 0 技术验证开始 ==========\n");

        // 示例 1 · 聊天：验证对话模型连通
        String answer = chatClientBuilder.build()
                .prompt("用一句话解释什么是 RAG")
                .call()
                .content();
        System.out.println("【示例1·聊天】" + answer + "\n");

        // 示例 2 · Embedding：验证向量模型连通，并打印维度（Day 4 建向量库要用）
        float[] vector = embeddingModel.embed("检索增强生成");
        System.out.println("【示例2·Embedding】维度=" + vector.length
                + "  前5个值=" + Arrays.toString(Arrays.copyOf(vector, 5)) + "\n");

        // 示例 3 · 工具调用：验证模型能自主调用 Java 方法
        String toolAnswer = chatClientBuilder.build()
                .prompt("北京现在几点？")
                .tools(new ClockTools())
                .call()
                .content();
        System.out.println("【示例3·工具调用】模型回答=" + toolAnswer);

        System.out.println("\n========== Day 0 技术验证结束 ==========\n");
    }
}
