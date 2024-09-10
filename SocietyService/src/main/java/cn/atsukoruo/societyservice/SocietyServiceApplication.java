package cn.atsukoruo.societyservice;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringSerializer;
import org.mybatis.spring.annotation.MapperScan;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.springframework.aop.framework.AopContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@MapperScan
@EnableAsync
@Slf4j
public class SocietyServiceApplication {
    // TODO 帖子发布功能
    // TODO 帖子删除功能
    // TODO 评论
    // TODO 点赞、取消点赞
    // TODO 获取关注的人、获取粉丝、获取关注数、获取粉丝数
    // 关注
    // 取关
    // 拉黑
    // 取消拉黑
    public static void main(String[] args) {
        SpringApplication.run(SocietyServiceApplication.class, args);
    }

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        ClusterServersConfig clusterServersConfig = config.useClusterServers()
                .addNodeAddress(
                        "redis://114.116.218.95:6379",
                        "redis://116.63.9.166:6379",
                        "redis://114.116.204.34:6379",
                        "redis://114.116.220.197:6379",
                        "redis://122.9.36.231:6379",
                        "redis://122.9.7.252:6379")
                .setSlaveConnectionMinimumIdleSize(1)
                .setMasterConnectionMinimumIdleSize(1)
                .setMasterConnectionPoolSize(8)
                .setSlaveConnectionPoolSize(8);
        clusterServersConfig.setPassword("grf.2001");//设置密码
        return Redisson.create(config);
    }


}
