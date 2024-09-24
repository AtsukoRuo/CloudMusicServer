package cn.atsukoruo.nettygateway.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.redisson.api.RedissonClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NettyServer {
    /**
     * channelID 到 Channel 的映射
     */
    final static Map<Long, Channel> channleMap= new HashMap<>();

    /**
     * serviceID 到 channelId 的映射
     */
    final static Map<Long, List<Long>> serviceChannel = new HashMap<>();

    private final int serverPort;
    private final int nettyId;
    private final RedissonClient redissonClient;
    private final ServerBootstrap serverBootstrap;
    private EventLoopGroup bossLoopGroup;
    private EventLoopGroup workerLoopGroup;
    private Channel serverChannel;

    public NettyServer(int port,
                       int nettyId,
                       RedissonClient redissonClient) {
        this.serverPort = port;
        serverBootstrap = new ServerBootstrap();
        this.redissonClient = redissonClient;
        this.nettyId = nettyId;
    }

    public void start() {
        // boss 相当于 Accept Reactor 中的线程池
        bossLoopGroup = new NioEventLoopGroup(1);
        // worker 相当于处理业务数据的 Reactor 中的线程池
        workerLoopGroup = new NioEventLoopGroup();

        try {
            // 1. 设置 Reactor 轮询组
            serverBootstrap.group(bossLoopGroup, workerLoopGroup);

            //2. 设置父通道的类型，就是以什么类型封装底层的 Java Nio 的 Channel
            serverBootstrap.channel(NioServerSocketChannel.class);

            //3. 设置监听端口
            serverBootstrap.localAddress(serverPort);

            //5. 装配 Channel 流水线
            serverBootstrap.childHandler(new WebSocketServerInitializer(redissonClient, nettyId));

            //6. 开始绑定服务器
            ChannelFuture channelFuture = serverBootstrap.bind().sync();
            serverChannel = channelFuture.channel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        serverChannel.close();
        bossLoopGroup.shutdownGracefully();
        workerLoopGroup.shutdownGracefully();
    }
}