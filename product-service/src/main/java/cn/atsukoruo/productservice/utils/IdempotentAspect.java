package cn.atsukoruo.productservice.utils;

import cn.atsukoruo.common.utils.Response;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;


@Aspect
@Component
public class IdempotentAspect {
    private final RedissonClient redissonClient;

    public IdempotentAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Pointcut("@annotation(cn.atsukoruo.productservice.utils.Idempotent)")
    public void idempotent() {}

    @Around("idempotent()")
    public Object methodAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        String param = idempotent.param();
        String value = null;
        String[] paramNames = ((CodeSignature)joinPoint.getSignature()).getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].toLowerCase().equals(param.toLowerCase())) {
                value = String.valueOf(args[i]);
                break;
            }
        }
        if (value == null) {
            return Response.fail("未携带 Token");
        }
        boolean hasToken = redissonClient.getBucket(value).delete();
        if (hasToken) {
            return joinPoint.proceed();
        }
        return Response.fail("重复的请求");
    }
}
