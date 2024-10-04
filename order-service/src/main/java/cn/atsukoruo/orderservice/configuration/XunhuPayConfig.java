package cn.atsukoruo.orderservice.configuration;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("xunhupay")
@Data
public class XunhuPayConfig {
    private String version;
    private String appid;
    private String notifyUrl;
    private String key;
    private String payUrl;
}
