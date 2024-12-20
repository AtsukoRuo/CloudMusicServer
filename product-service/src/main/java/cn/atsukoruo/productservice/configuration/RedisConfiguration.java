package cn.atsukoruo.productservice.configuration;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfiguration {
    @Bean
    public RedissonClient defaultRedissonClient() {
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
}