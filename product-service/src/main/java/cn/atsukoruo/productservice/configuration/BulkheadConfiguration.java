package cn.atsukoruo.productservice.configuration;


import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class BulkheadConfiguration {

    private BulkheadConfig defaultBulkheadConfig() {
        return BulkheadConfig.custom()
                .maxConcurrentCalls(10)
                .maxWaitDuration(Duration.ofMillis(500))
                .build();
    }

    @Bean("default-bulkhead-registry")
    public BulkheadRegistry defaultBulkheadRegistry() {
        return BulkheadRegistry.of(defaultBulkheadConfig());
    }
}
