package cn.atsukoruo.authorization;


import cn.atsukoruo.authorization.service.RegisterService;
import com.xiaoju.uemc.tinyid.client.utils.TinyId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
