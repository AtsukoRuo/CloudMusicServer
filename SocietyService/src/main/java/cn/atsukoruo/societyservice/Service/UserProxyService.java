package cn.atsukoruo.societyservice.Service;


import cn.atsukoruo.common.utils.Response;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class UserProxyService {
    private final RestClient.Builder restClientBuilder;
    private final RedissonClient redissonClient;

    public UserProxyService(
            RestClient.Builder restClientBuilder,
            RedissonClient redissonClient) {
        this.restClientBuilder = restClientBuilder;
        this.redissonClient = redissonClient;
    }
    private final static String AUTHORIZATION_SERVICE = "authorization-service";
    public boolean isInfluencer(int user) {
        RBucket<Integer> bucket =  redissonClient.getBucket(buildInfluencerKey(user));
        if (bucket.isExists()) {
            return bucket.get() == 1;
        }
        Response<?> response =  restClientBuilder.build().get().uri("http://{domain}/influencer?userId={id}",
                AUTHORIZATION_SERVICE, user).accept(MediaType.APPLICATION_JSON)
                .retrieve().body(Response.class);
        boolean result = response != null && (Boolean)response.getData();
        bucket.set(result ? 1 : 0);
        return result;
    }

    private String buildInfluencerKey(int user) {
        return "influencer:" + user;
    }
}
