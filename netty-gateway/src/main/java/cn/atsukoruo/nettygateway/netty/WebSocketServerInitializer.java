package cn.atsukoruo.nettygateway.netty;

import cn.atsukoruo.common.config.CommandConfig;
import cn.atsukoruo.common.entity.Message;
import cn.atsukoruo.common.utils.JsonUtils;
import cn.atsukoruo.nettygateway.tinyid.TinyId;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;


@Slf4j
public class WebSocketServerInitializer extends ChannelInitializer<Channel> {
    private final RedissonClient redissonClient;
    private final int nettyId;

    private Long id;
    private Long serviceId;

    public WebSocketServerInitializer(
            RedissonClient redissonClient,
            int nettyId) {
        this.redissonClient = redissonClient;
        this.nettyId = nettyId;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast(
                new HttpServerCodec(),
                new HttpObjectAggregator(65536),
                new WebSocketServerProtocolHandler("/cloud-music"),
                new TextFrameHandler(),
                new WebsocketIdHandler());
    }

    public class TextFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
        @Override
        public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
            String json = msg.text();
            Message message = JsonUtils.parseObject(json, Message.class);
            log.info("接收到的文本帧：" + message);
            switch (message.getCommand()) {
                case CommandConfig.REGISTER_SERVICE_ID -> {
                    handleRegister(message);
                }
                case CommandConfig.REPLAY -> {
                    handleReplay(message);
                }
                default -> {
                    log.info("不支持该类型的消息 " + message.getCommand());
                }
            }
        }

        private void handleReplay(Message message) {
            Long dest = message.getDest();
            Channel channel = NettyServer.channleMap.get(dest);
            channel.writeAndFlush(new TextWebSocketFrame(message.getPayload()));
        }

        private void handleRegister(Message message) {
            Long serviceId = Long.valueOf(message.getPayload());
            Long channelId= message.getChannelId();
            WebSocketServerInitializer.this.serviceId = serviceId;
            List<Long> channelIds = NettyServer.serviceChannel.get(serviceId);
            if (channelIds == null) {
                synchronized (NettyServer.serviceChannel) {
                    channelIds = new ArrayList<>();
                    NettyServer.serviceChannel.put(serviceId, channelIds);
                }
            }
            channelIds.add(channelId);
        }
    }


    public class WebsocketIdHandler extends ChannelInboundHandlerAdapter {


        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            log.info("连接已下线: " + ctx.channel().remoteAddress());
            NettyServer.channleMap.remove(id);
            if (serviceId != null) {
                // 说明这条连接是服务内部连接
                NettyServer.serviceChannel.get(serviceId).remove(id);
            }
            RBucket<Integer> bucket = redissonClient.getBucket(id.toString());
            bucket.delete();
            super.channelInactive(ctx);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
                Long id = TinyId.nextId();
                WebSocketServerInitializer.this.id = id;

                Message message = Message.builder()
                    .command(CommandConfig.DELIVERY_ID)
                    .payload(id.toString())
                    .build();
                String result = JsonUtils.toJson(message);
                ctx.writeAndFlush(new TextWebSocketFrame(result)).addListener((ChannelFutureListener) channelFuture -> {
                    log.info("握手已完成" + ctx.channel().remoteAddress());
                    if (channelFuture.isSuccess()) {
                        RBucket<Integer> bucket =  redissonClient.getBucket(id.toString());
                        bucket.set(nettyId);
                        NettyServer.channleMap.put(id, ctx.channel());
                    }
                });
            }
            super.userEventTriggered(ctx, evt);
        }
    }
}