package com.sgv.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "desktopTaskExecutor")
    public Executor desktopTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("sgv-async-worker-");
        executor.setDaemon(true);
        executor.initialize();
        return executor;
    }

    @Bean(name = "backgroundExecutorService")
    public ExecutorService backgroundExecutorService() {
        return Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r);
            t.setName("sgv-bg-pool-" + t.getId());
            t.setDaemon(true);
            return t;
        });
    }
}
