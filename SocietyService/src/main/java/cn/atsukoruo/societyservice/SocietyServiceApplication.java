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
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@MapperScan
@EnableAsync
@Slf4j
public class SocietyServiceApplication {
    // 帖子发布功能
    // TODO 帖子删除功能
    // TODO 评论
    // TODO 点赞、取消点赞
    // 获取关注的人、获取粉丝、获取关注数、获取粉丝数
    // 关注
    // 取关
    // 拉黑
    // 取消拉黑
    public static void main(String[] args) {
        SpringApplication.run(SocietyServiceApplication.class, args);
    }
}
