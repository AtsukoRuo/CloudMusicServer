package cn.atsukoruo.productservice.configuration;

import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;

@Configuration
public class HttpClientConfiguration {

    @LoadBalanced
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient restClient(RestClient.Builder builder) {
        return builder.baseUrl(null).requestInterceptor(new RestClientInterceptor()).build();
    }
}

@Slf4j
@Component
class RestClientInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        // 传递用户的 accessToken
        Object credential = SecurityContextHolder.getContext().getAuthentication().getCredentials();
        if (credential instanceof String jwt
            && org.springframework.util.StringUtils.hasText(jwt)) {
            request.getHeaders().add("cn-atsukoruo-accessToken", jwt);
        }

        // 传递全局事务 ID
        String xid = RootContext.getXID();
        if (!StringUtils.isEmpty(xid)) {
            log.info("要传递的 XID 为 " + xid);
            request.getHeaders().add(RootContext.KEY_XID, xid);
        }
        return execution.execute(request, body);
    }
}