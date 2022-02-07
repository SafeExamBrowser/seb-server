/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.async;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncServiceSpringConfig implements AsyncConfigurer {

    public static final String EXECUTOR_BEAN_NAME = "AsyncServiceExecutorBean";

    /** This ThreadPool is used for internal long running background tasks */
    @Bean(name = EXECUTOR_BEAN_NAME)
    public Executor threadPoolTaskExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(7);
        executor.setMaxPoolSize(42);
        executor.setQueueCapacity(11);
        executor.setThreadNamePrefix("asyncService-");
        executor.initialize();
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

    public static final String EXAM_API_EXECUTOR_BEAN_NAME = "ExamAPIAsyncServiceExecutorBean";

    /** This ThreadPool is used for SEB client connection establishment and
     * should be able to handle incoming bursts of SEB client connection requests (handshake)
     * when up to 1000 - 2000 clients connect at nearly the same time (start of an exam) */
    @Bean(name = EXAM_API_EXECUTOR_BEAN_NAME)
    public Executor examAPIThreadPoolTaskExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(200);
        executor.setMaxPoolSize(2000);
        executor.setQueueCapacity(2000);
        executor.setThreadPriority(Thread.MAX_PRIORITY);
        executor.setThreadNamePrefix("examService-");
        executor.initialize();
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }

    public static final String EXAM_API_PING_SERVICE_EXECUTOR_BEAN_NAME = "examAPIPingThreadPoolTaskExecutor";

    /** This ThreadPool is used for ping handling in a distributed setup and shall reject
     * incoming ping requests as fast as possible if there is to much load on the DB.
     * We prefer to loose a shared ping update and respond to the client in time over a client request timeout */
    @Bean(name = EXAM_API_PING_SERVICE_EXECUTOR_BEAN_NAME)
    public Executor examAPIPingThreadPoolTaskExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("SEBPingService-");
        executor.initialize();
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        final ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(false);
        threadPoolTaskScheduler.setThreadNamePrefix("SEB-Server-BgTask-");
        return threadPoolTaskScheduler;
    }

    @Override
    public Executor getAsyncExecutor() {
        return threadPoolTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

}
