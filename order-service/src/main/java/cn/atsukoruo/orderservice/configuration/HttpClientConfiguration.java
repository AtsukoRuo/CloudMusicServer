package cn.atsukoruo.orderservice.configuration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfiguration {
        @Bean
        RestClient restClient() {
            return RestClient.builder().build();
        }
}
