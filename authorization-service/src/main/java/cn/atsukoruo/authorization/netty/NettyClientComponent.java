package cn.atsukoruo.authorization.netty;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NettyClientComponent {

    // 同一个微服务下所有实例，共用 serviceId
    @Value("${netty.serviceId}")
    Long serviceId;

    private NettyClient nettyClient;

    @PostConstruct
    public void startServer() {
        nettyClient = new NettyClient(serviceId);
        try {
            nettyClient.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void stopServer() {
        if (nettyClient != null) {
            nettyClient.close();
        }
    }
}
