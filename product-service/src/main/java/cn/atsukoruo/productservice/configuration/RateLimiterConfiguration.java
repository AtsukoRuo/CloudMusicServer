package cn.atsukoruo.productservice.configuration;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimiterConfiguration {

    private RateLimiterConfig defaultConfig() {
        return RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(1))
                .limitForPeriod(10)
                .timeoutDuration(Duration.ofMillis(25))
                .build();
    }

    @Bean(name = "default-rate-limiter-registry")
    public RateLimiterRegistry defaultRateLimiterRegistry() {
        return RateLimiterRegistry.of(defaultConfig());
    }
}
