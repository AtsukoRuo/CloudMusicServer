package cn.atsukoruo.authorization.configuration;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfiguration {
    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.access-key.id}")
    private String accessKeyId;

    @Value("${oss.access-key.secret}")
    private String accessKeySecret;

    private OSS ossClient;

    @Bean
    public OSS defaultOssClient() {
        ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        return ossClient;
    }

    @PreDestroy
    public void close() {
        ossClient.shutdown();
    }
}
