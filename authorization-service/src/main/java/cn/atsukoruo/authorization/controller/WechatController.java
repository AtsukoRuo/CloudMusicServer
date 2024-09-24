package cn.atsukoruo.authorization.controller;

import cn.atsukoruo.authorization.service.WechatService;
import cn.atsukoruo.common.utils.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RequestMapping("wechat")
@RestController
@Slf4j
public class WechatController {
    private final WechatService wechatService;

    public WechatController(WechatService wechatService) {
        this.wechatService = wechatService;
    }

    @GetMapping
    public void get(@RequestParam(required = false) String signature,
                    @RequestParam(required = false) String timestamp,
                    @RequestParam(required = false) String nonce,
                    @RequestParam(required = false) String echostr,
                    HttpServletResponse response) throws IOException {
        // 这里的 WeChatMpService 是我们自定义的，继承 weixin-java-mp 包的 WxMpService 类，来做认证服务。下面会给出代码
        if (!this.wechatService.checkSignature(timestamp, nonce, signature)) {
            log.warn("接收到了未通过校验的微信消息");
            return;
        }
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(echostr);
        response.getWriter().flush();
        response.getWriter().close();
        log.info("配置成功");
    }



    @GetMapping("generateQrCode/{channelId}")
    public Response<Object> generateQrCode(
            @PathVariable("channelId") Long channelId
    ) throws WxErrorException {
        String token = wechatService.generateQrCode(channelId);
        return Response.success(token);
    }

    @GetMapping("bind/{user}/{channelId}")
    public Response<Object> bindVx(
            @PathVariable("user") Integer user,
            @PathVariable("channelId") Long channelId
    ) throws WxErrorException {
        String token = wechatService.bindVx(user, channelId);
        return Response.success(token);
    }


    @PostMapping(produces = "text/xml; charset=UTF-8")
    public void api(HttpServletRequest httpServletRequest,
                    HttpServletResponse httpServletResponse,
                    @RequestParam("signature") String signature,
                    @RequestParam(name = "encrypt_type", required = false) String encType,
                    @RequestParam(name = "msg_signature", required = false) String msgSignature,
                    @RequestParam("timestamp") String timestamp,
                    @RequestParam("nonce") String nonce) throws IOException {

        wechatService.api(httpServletRequest, httpServletResponse, signature, encType, msgSignature, timestamp, nonce);
    }
}
