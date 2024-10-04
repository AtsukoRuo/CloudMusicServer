package cn.atsukoruo.productservice.configuration;

import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
public class RetryConfiguration {

    private RetryConfig defaultRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(1000))
                .retryExceptions(IOException.class, TimeoutException.class)
                .failAfterMaxAttempts(true)
                .build();
    }

    @Bean("default-retry-registry")
    public RetryRegistry defaultRetryRegistry() {
        return RetryRegistry.of(defaultRetryConfig());
    }
}
