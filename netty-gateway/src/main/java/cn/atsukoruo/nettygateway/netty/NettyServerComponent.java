package cn.atsukoruo.nettygateway.netty;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NettyServerComponent {
    @Value("${netty.port}")
    private int port;

    @Value("${netty.id}")
    private int nettyId;

    private NettyServer nettyServer;

    private final RedissonClient redissonClient;

    public NettyServerComponent(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @PostConstruct
    public void startServer() {
        nettyServer = new NettyServer(port, nettyId, redissonClient);
        log.info("正在初始化 Netty");
        try {
            nettyServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("Netty 已成功启动");

        // 添加一个关闭钩子，清除 Redis 缓存
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (var entity :NettyServer.channleMap.entrySet() ) {
                redissonClient.getBucket(entity.getKey().toString()).delete();
            }
        }));
    }

    @PreDestroy
    public void stopServer() {
        if (nettyServer != null) {
            nettyServer.close();
        }
    }
}
