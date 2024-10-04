package cn.atsukoruo.nettygateway;

import cn.atsukoruo.zookeeperprimitives.ZkConfig;
import cn.atsukoruo.zookeeperprimitives.ZookeeperPrimitives;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WebsocketServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebsocketServerApplication.class, args);
    }

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        ClusterServersConfig clusterServersConfig = config.useClusterServers()
                .addNodeAddress(
                        "redis://114.116.218.95:6379",
                        "redis://116.63.9.166:6379",
                        "redis://114.116.204.34:6379",
                        "redis://114.116.220.197:6379")
                .setSlaveConnectionMinimumIdleSize(1)
                .setMasterConnectionMinimumIdleSize(1)
                .setMasterConnectionPoolSize(8)
                .setSlaveConnectionPoolSize(8);
        clusterServersConfig.setPassword("grf.2001");//设置密码
        return Redisson.create(config);
    }

    @Value("${netty.port}")
    private Integer port;
    @Value("${netty.exposed_url}")
    private String exposedUrl;
    @Value("${netty.id}")
    private Integer id;

    @Bean
    public ZookeeperPrimitives zookeeperPrimitives() throws Exception {
        ZkConfig zkConfig = new ZkConfig("114.116.204.34:2181,122.9.36.231:2181,116.63.9.166:2181", "/netty", 30000);
        ZookeeperPrimitives zk = new ZookeeperPrimitives(zkConfig);
        ZookeeperPrimitives.Metadata metadata = new ZookeeperPrimitives.Metadata(exposedUrl, port.toString(), id.toString());
        zk.register("websocket", metadata);
        return zk;
    }
}
