package com.docuagent.docuagent.config;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
@Configuration
public class StreamConfig {
    @Bean public ThreadPoolTaskExecutor qaStreamExecutor(){var pool=new ThreadPoolTaskExecutor();pool.setCorePoolSize(2);pool.setMaxPoolSize(4);pool.setQueueCapacity(8);pool.setThreadNamePrefix("qa-stream-");pool.setWaitForTasksToCompleteOnShutdown(false);return pool;}
}
