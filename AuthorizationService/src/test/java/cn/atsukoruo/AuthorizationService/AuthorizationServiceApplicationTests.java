package cn.atsukoruo.AuthorizationService;


import cn.atsukoruo.AuthorizationService.Entity.User;
import cn.atsukoruo.AuthorizationService.Repository.UserMapper;
import com.aliyun.oss.OSS;
import org.junit.jupiter.api.Test;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.sql.Timestamp;


@SpringBootTest
class AuthorizationServiceApplicationTests {

}
