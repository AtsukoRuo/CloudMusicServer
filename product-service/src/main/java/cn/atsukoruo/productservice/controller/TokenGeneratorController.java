package cn.atsukoruo.productservice.controller;


import cn.atsukoruo.common.utils.Response;
import cn.atsukoruo.productservice.utils.Idempotent;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class TokenGeneratorController {
    private final RedissonClient redissonClient;
    public TokenGeneratorController(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @GetMapping("/idempotent")
    public Response<Object> getIdempotentToken() {
        String token = UUID.randomUUID().toString();
        redissonClient.getBucket(token).set(0, 30, TimeUnit.MINUTES);
        return Response.success(token);
    }

    @Idempotent
    @GetMapping("/idempotent/test")
    public Response<Object> testIdempotent(
            @RequestParam("token") String token
    ) {
        return Response.success("OK");
    }
}
