package cn.atsukoruo.AuthorizationService.Handler;

import cn.atsukoruo.AuthorizationService.Service.UserService;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UnsubscribeHandler implements WxMpMessageHandler {
    private final UserService userService;

    public UnsubscribeHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public WxMpXmlOutMessage handle(
            WxMpXmlMessage wxMessage,
            Map<String, Object> context,
            WxMpService wxMpService, WxSessionManager sessionManager) throws WxErrorException {
        String vxOpenId =  wxMessage.getFromUser();
        userService.deleteVxOpenIdField(vxOpenId);
        return null;
    }
}
