package cn.atsukoruo.societyservice.Configuration;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfiguration {
    @LoadBalanced
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
