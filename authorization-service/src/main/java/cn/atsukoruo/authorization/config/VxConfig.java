package cn.atsukoruo.authorization.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("vx")
@Configuration
@Data
public class VxConfig {
    private String appId;
    private String secret;
    private String token;
}