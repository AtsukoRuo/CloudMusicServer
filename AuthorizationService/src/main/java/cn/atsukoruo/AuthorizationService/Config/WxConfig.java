package cn.atsukoruo.AuthorizationService.Config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("wx.mp")
@Configuration
@Data
public class WxConfig {
    private String appId;
    private String secret;
    private String token;
}