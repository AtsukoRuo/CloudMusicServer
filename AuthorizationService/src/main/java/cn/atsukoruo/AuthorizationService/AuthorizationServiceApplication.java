package cn.atsukoruo.AuthorizationService;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.mybatis.spring.annotation.MapperScan;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@EnableTransactionManagement
@MapperScan
public class AuthorizationServiceApplication implements DisposableBean {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServiceApplication.class, args);
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("noop", NoOpPasswordEncoder.getInstance());
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        String encodingId = "bcrypt";
        return new DelegatingPasswordEncoder(encodingId, encoders);
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

    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.access-key.id}")
    private String accessKeyId;

    @Value("${oss.access-key.secret}")
    private String accessKeySecret;

    private OSS ossClient;

    @Bean
    public OSS ossClient() {
        ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        return ossClient;
    }

    public void destroy() {
        ossClient.shutdown();
    }
}
