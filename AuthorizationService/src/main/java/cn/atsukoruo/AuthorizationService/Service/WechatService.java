package cn.atsukoruo.AuthorizationService.Service;

import cn.atsukoruo.AuthorizationService.Config.WxConfig;
import cn.atsukoruo.AuthorizationService.Handler.ScanHandler;
import cn.atsukoruo.AuthorizationService.Handler.SubscribeHandler;
import cn.atsukoruo.AuthorizationService.Handler.UnsubscribeHandler;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static me.chanjar.weixin.common.api.WxConsts.EventType.*;
import static me.chanjar.weixin.common.api.WxConsts.XmlMsgType.EVENT;

@Slf4j
@Service
public class WechatService extends WxMpServiceImpl {
    private final WxConfig wxConfig;



    public WechatService(WxConfig wxConfig,
                         ScanHandler scanHandler,
                         SubscribeHandler subscribeHandler,
                         UnsubscribeHandler unsubscribeHandler) {
        this.wxConfig = wxConfig;
        this.scanHandler = scanHandler;
        this.subscribeHandler= subscribeHandler;
        this.unsubscribeHandler = unsubscribeHandler;
    }

    private final ScanHandler scanHandler;
    private final SubscribeHandler subscribeHandler;
    private final UnsubscribeHandler unsubscribeHandler;


    @PostConstruct
    public void init() {
        final WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        config.setAppId(wxConfig.getAppId());
        config.setSecret(wxConfig.getSecret());
        config.setToken(wxConfig.getToken());
        super.setWxMpConfigStorage(config);
        this.refreshRouter();
    }

    public String generateQrCode(Long channelId) throws WxErrorException {
        String scene = "login_" + channelId.toString();
        WxMpQrCodeTicket wxMpQrCodeTicket = super.getQrcodeService().qrCodeCreateTmpTicket(scene, 100 * 60);
        return wxMpQrCodeTicket.getTicket();
    }

    public String bindVx(Integer user, Long channelId) throws WxErrorException {
        String scene = "bind_" + user.toString() + "_" + channelId;
        WxMpQrCodeTicket wxMpQrCodeTicket = super.getQrcodeService().qrCodeCreateTmpTicket(scene, 100 * 60);
        return wxMpQrCodeTicket.getTicket();
    }

    private WxMpMessageRouter router;

    private void refreshRouter() {
        final WxMpMessageRouter newRouter = new WxMpMessageRouter(this);

        // 关注事件
        newRouter.rule().async(false)
                .msgType(EVENT).event(SUBSCRIBE)
                .handler(this.subscribeHandler).end();

        // 取消关注事件
        newRouter.rule().async(false)
                .msgType(EVENT).event(UNSUBSCRIBE)
                .handler(this.unsubscribeHandler).end();

        // 扫码事件
        newRouter.rule().async(false)
                .msgType(EVENT).event(SCAN)
                .handler(this.scanHandler).end();

        this.router = newRouter;
    }

    /**
     * 微信事件通过这个入口进来
     * 根据不同事件，调用不同handler
     */
    public WxMpXmlOutMessage route(WxMpXmlMessage message) {
        try {
            return this.router.route(message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public void api(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String signature, String encType, String msgSignature, String timestamp, String nonce) throws IOException {
        if (!checkSignature(timestamp, nonce, signature)) {
            log.warn("接收到了未通过校验的微信消息");
            return;
        }

        // 构建 Body
        BufferedReader bufferedReader = httpServletRequest.getReader();
        StringBuilder requestBodyBuilder = new StringBuilder();
        String str;
        while ((str = bufferedReader.readLine()) != null) {
            requestBodyBuilder.append(str);
        }
        String requestBody = requestBodyBuilder.toString();

        log.info("\n接收微信请求：[signature=[{}], encType=[{}], msgSignature=[{}],"
                        + " timestamp=[{}], nonce=[{}], requestBody=[\\n{}\\n]",
                signature, encType, msgSignature, timestamp, nonce, requestBody);


        String out = null;
        if (encType == null) {
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(requestBody);
            // 从这里进行路由分发
            WxMpXmlOutMessage outMessage = route(inMessage);
            if (outMessage == null) {
                httpServletResponse.getOutputStream().write(new byte[0]);
                httpServletResponse.flushBuffer();
                httpServletResponse.getOutputStream().close();
                return;
            }
            out = outMessage.toXml();
        }

        httpServletResponse.getOutputStream().write(out.getBytes(StandardCharsets.UTF_8));
        httpServletResponse.flushBuffer();
        httpServletResponse.getOutputStream().close();
    }


}
