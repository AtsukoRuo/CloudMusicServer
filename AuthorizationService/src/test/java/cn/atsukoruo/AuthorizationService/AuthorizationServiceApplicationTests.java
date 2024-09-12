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
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserMapper userMapper;


    @Test
    public void insertTestUser() {
        for (int i = 0; i < 100; i++) {
            User user = buildUser("test" + i, "test" + i, "grf.2001", "");
            userMapper.insertUser(user);
        }
    }

    private User buildUser(String username, String nickname, String password, String imgUrl) {
        return User.builder()
                .username(username)
                .nickname(nickname)
                .password(passwordEncoder.encode("{bcrypt}"+password))
                .role("user")
                .isBanned(false)
                .isInfluencer(false)
                .avatar_url(imgUrl)
                .createTime(new Timestamp(System.currentTimeMillis()))
                .build();
    }

    @Autowired
    RedissonClient redissonClient;

    @Test
    public void getAllKeys() {
        RKeys rKeys = redissonClient.getKeys();
        Iterable<String> keys = rKeys.getKeys();
        for (String key : keys) {
            System.out.println(key);
        }
    }

    @Test
    public void isBanned() throws Exception {
        User user =  userMapper.selectById(136);
        if (!user.isBanned()) {
            throw new Exception("不合状态");
        }
    }


    @Value("${oss.endpoint}")
    private String endpoint;
    @Value("${oss.access-key.id}")
    private String accessKeyId;
    @Value("${oss.access-key.secret}")
    private String accessKeySecret;

    @Autowired
    OSS ossClient;
}
