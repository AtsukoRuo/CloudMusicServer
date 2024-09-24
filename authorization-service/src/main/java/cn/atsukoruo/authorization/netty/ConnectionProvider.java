package cn.atsukoruo.authorization.netty;


import cn.atsukoruo.common.entity.Message;
import cn.atsukoruo.common.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;


@Component
public class ConnectionProvider {

    private final RedissonClient redissonClient;
    public ConnectionProvider(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public  void sendMessage(Message message, Long channelId) {
        RBucket<Integer> bucket = redissonClient.getBucket(channelId.toString());
        var gatewayId =  bucket.get();
        if (gatewayId == null) {
            return;
        }
        Channel channel = NettyClient.channelMap.get(gatewayId);
        if (channel != null) {
            String msg = JsonUtils.toJson(message);
            channel.writeAndFlush(new TextWebSocketFrame(msg));
        }
    }
}
