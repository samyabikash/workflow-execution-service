package com.growthloops.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Bounded executor for {@code @Async} workflow runs.
 * <p>
 * Without this, {@code @EnableAsync} falls back to {@code SimpleAsyncTaskExecutor},
 * which spawns a brand-new unbounded thread per execution — no pooling and no
 * backpressure. A bounded pool with a queue caps concurrency under load.
 */
@Configuration
public class AsyncConfig {

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("wf-exec-");
        executor.initialize();
        return executor;
    }
}
