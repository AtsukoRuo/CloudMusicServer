package cn.atsukoruo.AuthorizationService.Netty;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NettyClientComponent {
    @Value("${netty.uri}")
    String uris;

    @Value("${netty.serviceId}")
    Long serviceId;

    private NettyClient nettyClient;

    @PostConstruct
    public void startServer() {
        nettyClient = new NettyClient(uris, serviceId);
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
