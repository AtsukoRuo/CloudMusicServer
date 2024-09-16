package cn.atsukoruo.societyservice.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolExecutorConfig {

    private static final int CORE_THREAD_SIZE = Runtime.getRuntime().availableProcessors() + 1;

    private static final int MAX_THREAD_SIZE = Runtime.getRuntime().availableProcessors() * 2 + 1;

    private static final int WORK_QUEUE = 1000;

    private static final int KEEP_ALIVE_SECONDS = 60;

    @Bean("taskExecutor")
    public Executor taskExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_THREAD_SIZE);
        executor.setMaxPoolSize(MAX_THREAD_SIZE);
        executor.setQueueCapacity(WORK_QUEUE);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix("task-thread-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // 新增线程装饰器
        executor.setTaskDecorator(new BusinessContextDecorator());
        executor.initialize();
        return executor;
    }

    @Bean("message-thread-pool")
    public Executor messageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_THREAD_SIZE);
        executor.setMaxPoolSize(MAX_THREAD_SIZE);
        executor.setQueueCapacity(WORK_QUEUE);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix("post-thread-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());
    }
}

class BusinessContextDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        return runnable::run;
    }
}