package cn.atsukoruo.AuthorizationService.Handler;


import cn.atsukoruo.AuthorizationService.Netty.ConnectionProvider;
import cn.atsukoruo.AuthorizationService.Service.UserService;
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
