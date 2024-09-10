package cn.atsukoruo.AuthorizationService;


import cn.atsukoruo.AuthorizationService.Repository.UserMapper;
import cn.atsukoruo.common.entity.User;
import com.aliyun.oss.OSS;
import net.bytebuddy.implementation.bytecode.Throw;
import org.junit.jupiter.api.Test;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Date;


@SpringBootTest
class AuthorizationServiceApplicationTests {
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserMapper userMapper;


    @Test
    public void insertUser() {
        User user = buildUser("123", "123123", "123123", "123123");

        userMapper.insertUser(user);
        System.out.println(user.getId());
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
                .createTime(new Date(System.currentTimeMillis()))
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
        if (user.isBanned() == false) {
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
