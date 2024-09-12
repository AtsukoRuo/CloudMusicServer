package cn.atsukoruo.societyservice.Configuration;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class HttpClientConfiguration {

    @LoadBalanced
    @Bean
    public RestTemplate initRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // 中文乱码，主要是 StringHttpMessageConverter的默认编码为ISO导致的
        List<HttpMessageConverter<?>> list = restTemplate.getMessageConverters();
        for (HttpMessageConverter<?> converter : list) {
            if (converter instanceof StringHttpMessageConverter) {
                ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8);
                break;
            }
        }

        OkHttpClient okHttpClient = okHttp3Client();
        restTemplate.setRequestFactory(new OkHttp3ClientHttpRequestFactory(okHttpClient));
        return new RestTemplate();
    }

    public OkHttpClient okHttp3Client() {
        return new OkHttpClient().newBuilder()
                .connectionPool(pool())
                .connectTimeout(10, TimeUnit.SECONDS)
                .callTimeout(5, TimeUnit.MINUTES)
                .hostnameVerifier((s, sslSession) -> true)
                // 禁止重定向
                .followRedirects(false)
                .build();
    }

    public ConnectionPool pool() {
        return new ConnectionPool(10, 2, TimeUnit.MINUTES);
    }
}
