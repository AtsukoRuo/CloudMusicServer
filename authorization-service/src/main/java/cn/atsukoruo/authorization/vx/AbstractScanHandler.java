package cn.atsukoruo.authorization.vx;

import cn.atsukoruo.authorization.entity.User;
import cn.atsukoruo.authorization.netty.ConnectionProvider;
import cn.atsukoruo.authorization.service.UserService;
import cn.atsukoruo.common.config.CommandConfig;
import cn.atsukoruo.common.config.ErrorCodeConfig;
import cn.atsukoruo.common.entity.Message;
import cn.atsukoruo.common.utils.JsonUtils;
import cn.atsukoruo.common.utils.Response;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
public class AbstractScanHandler {
    private final UserService userService;
    private final ConnectionProvider connectionProvider;
    public AbstractScanHandler(UserService userService,
                            ConnectionProvider connectionProvider) {
        this.userService = userService;
        this.connectionProvider = connectionProvider;
    }

    public WxMpXmlOutMessage handle(
            WxMpXmlMessage wxMessage,
            Map<String, Object> context,
            WxMpService wxMpService, WxSessionManager sessionManager) throws WxErrorException {

        String key = wxMessage.getEventKey();
        WxMpXmlOutMessage ret = WxMpXmlOutMessage.TEXT().content("感谢关注，祝您生活愉快!")
                .fromUser(wxMessage.getToUser()).toUser(wxMessage.getFromUser())
                .build();
        if (key.isEmpty() || key.isBlank()) {
            return ret;
        }

        log.info("key is " + key);
        if (key.startsWith("qrscene_")) {
            key = key.substring(8);
        }
        String[] strArray = key.split("_");
        String event = strArray[0];
        String vxOpenId =  wxMessage.getFromUser();
        String content = "";
        switch (event) {
            case "login" -> content = handleLoginEvent(strArray, vxOpenId);
            case "bind" -> content = handleBindEvent(strArray, vxOpenId);
            default -> content = "暂不支持该功能";
        }
        return WxMpXmlOutMessage.TEXT().content(content)
                .fromUser(wxMessage.getToUser()).toUser(wxMessage.getFromUser())
                .build();
    }

    private String handleLoginEvent(String[] keys, String vxOpenId) {
        Long channelId = Long.valueOf(keys[1]);
        User user = userService.getUserByVxOpenId(vxOpenId);
        String content;
        String resp;
        if (user == null) {
            Response<Object> response = Response.fail(ErrorCodeConfig.UNBIND_VX, "未绑定微信");
            resp = JsonUtils.toJson(response);
            content = "还未绑定微信";
        } else {
            Response<Object> response = Response.success();
            resp = JsonUtils.toJson(response);
            content =  "登录成功";
        }
        Message message = Message.builder()
                .command(CommandConfig.REPLAY)
                .dest(channelId)
                .payload(resp)
                .build();
        connectionProvider.sendMessage(message, channelId);
        return content;
    }

    private String handleBindEvent(String[] keys, String vxOpenId) {
        Long channelId = Long.valueOf(keys[2]);
        Integer user = Integer.valueOf(keys[1]);
        String id = userService.getVxOpenIdFromUser(user);
        String resp;
        String content;
        if (StringUtils.hasText(id)) {
            Response<Object> response = Response.fail(ErrorCodeConfig.DUPLICATED_BIND_VX, "已绑定过微信");
            resp = JsonUtils.toJson(response);
            content = "已绑定过该微信了";
        } else {
            userService.bindVxOpenId(user, vxOpenId);
            Response<Object> response = Response.success();
            resp = JsonUtils.toJson(response);
            content = "绑定成功";
        }
        Message message = Message.builder()
                .command(CommandConfig.REPLAY)
                .dest(channelId)
                .payload(resp)
                .build();
        connectionProvider.sendMessage(message, channelId);
        return content;
    }
}
