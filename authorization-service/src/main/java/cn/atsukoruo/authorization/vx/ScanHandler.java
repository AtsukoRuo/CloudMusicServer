package cn.atsukoruo.authorization.vx;

import cn.atsukoruo.authorization.netty.ConnectionProvider;
import cn.atsukoruo.authorization.service.UserService;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import org.springframework.stereotype.Component;
;

@Component
public class ScanHandler extends AbstractScanHandler implements WxMpMessageHandler {
    public ScanHandler(UserService userService, ConnectionProvider connectionProvider) {
        super(userService, connectionProvider);
    }
}
