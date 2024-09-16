package cn.atsukoruo.AuthorizationService;


import cn.atsukoruo.AuthorizationService.Entity.User;
import cn.atsukoruo.AuthorizationService.Repository.UserMapper;
import cn.atsukoruo.AuthorizationService.Service.RegisterService;
import cn.atsukoruo.AuthorizationService.Service.UserService;
import com.aliyun.oss.OSS;
import com.xiaoju.uemc.tinyid.client.utils.TinyId;
import org.junit.jupiter.api.Test;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;


@SpringBootTest
class AuthorizationServiceApplicationTests {
    @Autowired
    RegisterService registerService;

    @Test
    public void createNewUser() throws Exception, RuntimeException {
        for (int i = 0; i < 50; i++) {
            registerService.register("test" + i, "test" + i, "grf.2001", null);
        }
    }

    @Test
    public void tinyIdTest() {
        List<Long> ids = TinyId.nextId("test", 10);
        for (Long id : ids) {
            System.out.println(id);
        }
    }


}
