package cn.atsukoruo.authorization.vx;


import cn.atsukoruo.authorization.netty.ConnectionProvider;
import cn.atsukoruo.authorization.service.UserService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class SubscribeHandler extends AbstractScanHandler implements WxMpMessageHandler {
    public SubscribeHandler(UserService userService, ConnectionProvider connectionProvider) {
        super(userService, connectionProvider);
    }
}
