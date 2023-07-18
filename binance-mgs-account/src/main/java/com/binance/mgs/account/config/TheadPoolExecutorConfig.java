package com.binance.mgs.account.config;

import com.binance.accountanalyze.concurrent.LogAndDiscardPolicy;
import com.binance.platform.pool.threadpool.DynamicExecutor;
import com.binance.platform.pool.threadpool.DynamicExecutors;
import com.binance.platform.pool.threadpool.ext.RejectHandlerProxy;
import com.binance.platform.pool.threadpool.ext.VariableLinkedBlockingQueue;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Log4j2
@Configuration
public class TheadPoolExecutorConfig {

    @Bean("reCaptchaExecutor")
    public static ThreadPoolExecutor reCaptchaExecutor() {
        return new ThreadPoolExecutor(1, 5, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("account-reCaptcha-assessment-pool-%d").build(),
                new CustomDiscardPolicy());
    }

    @Bean("bCaptchaExecutor")
    public static ThreadPoolExecutor bCaptchaExecutor() {
        return new ThreadPoolExecutor(1, 5, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("account-bCaptcha-assessment-pool-%d").build(),
                new CustomDiscardPolicy());
    }

    @Bean("gtExecutor")
    public static ThreadPoolExecutor gtExecutor() {
        return new ThreadPoolExecutor(1, 5, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("account-gt-assessment-pool-%d").build(),
                new CustomDiscardPolicy());
    }

    @Bean("subUserBalanceExecutor")
    public static ThreadPoolExecutor subUserBalanceExecutor() {
        return new ThreadPoolExecutor(5, 20, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1024),
                new ThreadFactoryBuilder().setNameFormat("subUser-asset-balance-pool-%d").build(),
                new CustomDiscardPolicy());
    }

    @Bean("accountDefenseExecutor")
    public static ThreadPoolExecutor accountDefenseExecutor() {
        return new ThreadPoolExecutor(5, 5, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("account-defense-before-pool-%d").build(),
                new CustomDiscardPolicy());
    }

    @Bean("accountDefenseAfterServiceExecutor")
    public static ThreadPoolExecutor accountDefenseAfterServiceExecutor() {
        return new ThreadPoolExecutor(5, 5, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("account-defense-after-pool-%d").build(),
                new CustomDiscardPolicy());
    }

    public static class CustomDiscardPolicy extends ThreadPoolExecutor.DiscardPolicy {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            log.info("customDiscardPolicy rejectedExecution");
            // 使用futureTask过程中，命中拒绝策略后，希望能快速丢弃任务，而不是阻塞等待
            // cancel之后，future.get() 会抛出 CancellationException，可根据需要进行处理
            if (r instanceof FutureTask) {
                ((FutureTask<?>) r).cancel(true);
            }
            super.rejectedExecution(r, e);
        }
    }

    @Bean("methodAnalyzeExecutor")
    public DynamicExecutor methodAnalyzeExecutor() {
        return DynamicExecutors.custom("method-analyze-executor")
                .transmittable()
                .setCoreSize(1)
                .setMaxSize(3)
                .setKeepAliveTime(60, TimeUnit.SECONDS)
                .setBlockQueue(new VariableLinkedBlockingQueue<>(1000))
                .setRejectedExecutionHandler(RejectHandlerProxy.getProxy(new CustomDiscardPolicy()))
                .build();
    }

    @Bean("accountAnalyzeSecurityChallengeExecutor")
    public DynamicExecutor accountAnalyzeSecurityChallengeExecutor() {
        return DynamicExecutors.custom("account-analyze-security-challenge-executor")
                .setCoreSize(5)
                .setMaxSize(10)
                .setKeepAliveTime(60, TimeUnit.SECONDS)
                .setBlockQueue(new VariableLinkedBlockingQueue<>(1000))
                .setRejectedExecutionHandler(RejectHandlerProxy.getProxy(new CustomDiscardPolicy()))
                .build();
    }
}
