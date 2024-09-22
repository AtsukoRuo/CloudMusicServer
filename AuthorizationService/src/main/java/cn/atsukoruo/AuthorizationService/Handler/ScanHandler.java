package cn.atsukoruo.AuthorizationService.Handler;

import cn.atsukoruo.AuthorizationService.Netty.ConnectionProvider;
import cn.atsukoruo.AuthorizationService.Service.UserService;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import org.springframework.stereotype.Component;
;

@Component
public class ScanHandler extends AbstractScanHandler implements WxMpMessageHandler {
    public ScanHandler(UserService userService, ConnectionProvider connectionProvider) {
        super(userService, connectionProvider);
    }
}
