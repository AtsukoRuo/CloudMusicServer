package cn.atsukoruo.authorization.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class NettyClient {

    /**
     * Netty Server ID 到 Connection 的映射
     */
    public final static Map<Integer, Channel> channelMap = new HashMap<>();
    private final List<String> serverUris;
    private final Long serviceId;
    private  EventLoopGroup group;

    public NettyClient(String uris, Long serviceId) {
        serverUris = Arrays.stream(uris.split(",")).toList();
        this.serviceId = serviceId;
    }

    public void start() {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();

        bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast("http-codec",new HttpClientCodec());
                        pipeline.addLast("aggregator",new HttpObjectAggregator(1024*1024*10));
                        pipeline.addLast("hookedHandler", new WebSocketClientHandler(serviceId));
                    }
                });

        int id = 1;
        for (var uri : serverUris) {
            connect(uri, bootstrap, id);
            id++;
        }
    }

    private void connect(String uri, Bootstrap bootstrap, Integer id) {
        try {
            URI websocketURI = new URI(uri);
            HttpHeaders httpHeaders = new DefaultHttpHeaders();
            //进行握手
            WebSocketClientHandshaker handshake = WebSocketClientHandshakerFactory
                    .newHandshaker(websocketURI, WebSocketVersion.V13, (String) null, true, httpHeaders);
            final Channel channel = bootstrap.connect(websocketURI.getHost(), websocketURI.getPort()).sync().channel();
            WebSocketClientHandler handler = (WebSocketClientHandler) channel.pipeline().get("hookedHandler");
            handler.setHandshake(handshake);
            handshake.handshake(channel);
            //阻塞等待是否握手成功
            handler.handshakeFuture().sync();
            log.debug("与 " + uri + " 握手成功");
            channelMap.put(id, channel);
        } catch (Exception e) {
            log.debug("与 " + uri + " 握手失败");
            log.warn(e.toString());
        }
    }
    public void close() {
        group.shutdownGracefully();
    }
}