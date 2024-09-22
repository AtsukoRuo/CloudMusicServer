package cn.atsukoruo.AuthorizationService.Netty;

import cn.atsukoruo.common.config.CommandConfig;
import cn.atsukoruo.common.entity.Message;
import cn.atsukoruo.common.utils.JsonUtils;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private final Long serviceId;

    private Long channelId;
    public WebSocketClientHandler(Long serviceId) {
        this.serviceId = serviceId;
    }

    //握手的状态信息
    WebSocketClientHandshaker handshake;
    //netty自带的异步处理
    ChannelPromise handshakeFuture;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        FullHttpResponse response;
        //进行握手操作
        if (!this.handshake.isHandshakeComplete()) {
            try {
                response = (FullHttpResponse)msg;
                //握手协议返回，设置结束握手
                this.handshake.finishHandshake(ch, response);
                //设置成功
                this.handshakeFuture.setSuccess();
                log.info("服务端的消息：" + response.headers());
            } catch (WebSocketHandshakeException var7) {
                FullHttpResponse res = (FullHttpResponse)msg;
                String errorMsg = String.format("握手失败, status: %s, reason: %s", res.status(), res.content().toString(CharsetUtil.UTF_8));
                this.handshakeFuture.setFailure(new Exception(errorMsg));
            }
        } else if (msg instanceof FullHttpResponse) {
            response = (FullHttpResponse)msg;
            throw new IllegalStateException("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        } else {
            // 接收服务端的消息
            WebSocketFrame frame = (WebSocketFrame)msg;
            if (Objects.requireNonNull(frame) instanceof TextWebSocketFrame textWebSocketFrame) {
                handleTextWebsocketFrame(textWebSocketFrame, ctx);
            }
        }
    }

    private void handleTextWebsocketFrame(TextWebSocketFrame textWebSocketFrame, ChannelHandlerContext ctx) {
        log.info("客户端接收到的消息: " + textWebSocketFrame.text());
        Message message = JsonUtils.parseObject(textWebSocketFrame.text(), Message.class);
        switch (message.getCommand()) {
            case CommandConfig.DELIVERY_ID -> {
                handleDelivery(message, ctx);
            }
            default -> {
                log.info("不支持该类型的消息" + message.getCommand());
            }
        }
    }

    private void handleDelivery(Message message, ChannelHandlerContext ctx) {
        this.channelId = Long.valueOf(message.getPayload());
        Message msg = Message.builder()
                .command(CommandConfig.REGISTER_SERVICE_ID)
                .channelId(this.channelId)
                .payload(serviceId.toString())
                .build();
        String result = JsonUtils.toJson(msg);
        ctx.writeAndFlush(new TextWebSocketFrame(result));
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("与服务端连接成功");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("主机关闭");
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("连接异常："+cause.getMessage());
        ctx.close();
    }

    public void handlerAdded(ChannelHandlerContext ctx) {
        this.handshakeFuture = ctx.newPromise();
    }

    public WebSocketClientHandshaker getHandshake() {
        return handshake;
    }

    public void setHandshake(WebSocketClientHandshaker handshake) {
        this.handshake = handshake;
    }

    public ChannelPromise getHandshakeFuture() {
        return handshakeFuture;
    }

    public void setHandshakeFuture(ChannelPromise handshakeFuture) {
        this.handshakeFuture = handshakeFuture;
    }

    public ChannelFuture handshakeFuture() {
        return this.handshakeFuture;
    }
}
