package com.binance.mgs.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by nash
 *
 * 建议不同业务创建不同的异步执行器，起到线程隔离的作用
 */
@Configuration
@EnableAsync
public class ExecutorConfig {

    private int corePoolSize = 2;
    private int maxPoolSize = 10;
    private int queueCapacity = 1000;

    /**
     * 普通http调用异步执行器，通用的
     * 
     * @return
     */
    @Bean
    public Executor simpleRequestAsync() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("simpleRequestAsync-");
        // 拒绝策略：直接丢弃任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 安全相关呢业务异步执行器
     * 
     * @return
     */
    @Bean
    public Executor securityAsync() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize * 2);
        executor.setMaxPoolSize(maxPoolSize * 2);
        executor.setQueueCapacity(queueCapacity * 100);
        executor.setThreadNamePrefix("securityAsync-");
        // 拒绝策略：直接丢弃任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }

}
