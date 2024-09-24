package cn.atsukoruo.musicservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolExecutorConfiguration {


    private static final int CORE_THREAD_SIZE = Runtime.getRuntime().availableProcessors() + 1;

    private static final int MAX_THREAD_SIZE = Runtime.getRuntime().availableProcessors() * 2 + 1;

    private static final int WORK_QUEUE = 1000;

    private static final int KEEP_ALIVE_SECONDS = 60;


    @Bean("canal-thread-pool")
    public Executor canalTaskExecutor() {
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

}
