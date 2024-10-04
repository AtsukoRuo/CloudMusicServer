package cn.atsukoruo.productservice.service;

import cn.atsukoruo.common.utils.Response;
import cn.atsukoruo.productservice.entity.Product;
import cn.atsukoruo.productservice.exception.OversellException;
import cn.atsukoruo.productservice.repository.ProductMapper;
import cn.atsukoruo.productservice.utils.Idempotent;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.util.function.Supplier;

@Slf4j
@Service
public class ProductService {
    private final ProductMapper productMapper;
    private final RedissonClient redissonClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final RetryRegistry retryRegistry;
    private final RestClient restClient;


    public ProductService(ProductMapper productMapper,
                          RestClient restClient,
                          RedissonClient redissonClient,
                          @Qualifier("default-circuit-breaker-registry") CircuitBreakerRegistry circuitBreakerRegistry,
                          @Qualifier("default-rate-limiter-registry") RateLimiterRegistry rateLimiterRegistry,
                          @Qualifier("default-bulkhead-registry") BulkheadRegistry bulkheadRegistry,
                          @Qualifier("default-retry-registry") RetryRegistry retryRegistry
    ) {
        this.productMapper = productMapper;
        this.restClient = restClient;
        this.redissonClient = redissonClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.retryRegistry = retryRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
    }


    private final static String IDENTITY_METHOD_ADD_PRODUCT = "addProduct";
    @GlobalTransactional
    public void addProduct(String name, String price, String introduction, Integer initAmount) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(IDENTITY_METHOD_ADD_PRODUCT);
        circuitBreaker.getEventPublisher()
                .onSuccess(env -> log.info("【onSuccess】" + env.getEventType().name()))
                .onError(env -> log.info("【onError】" + env.getEventType().name()));
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(IDENTITY_METHOD_ADD_PRODUCT);
        Retry retry = retryRegistry.retry(IDENTITY_METHOD_ADD_PRODUCT);

        Supplier<Boolean> supplier = () -> doAddProduct(name, price, introduction, initAmount);
        Supplier<Boolean> decoratedSupplier = Decorators.ofSupplier(supplier)
                .withCircuitBreaker(circuitBreaker)
                .withRateLimiter(rateLimiter)
                .withRetry(retry)
                .decorate();

        decoratedSupplier.get();
    }

    private boolean doAddProduct(String name, String price, String introduction, Integer initAmount) {
        Product product = Product.builder().name(name)
                .price(new BigDecimal(price))
                .introduction(introduction)
                .build();
        productMapper.insertProduct(product);
        Integer id = product.getId();
        log.info(id.toString());
        restClient.post().uri(
                        "http://inventory-service/inventory?productId={productId}&amount={amount}", id, initAmount)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Response.class);
        return true;
    }

    //@Idempotent
    @GlobalTransactional
    public Object orderProduct(Integer productId, Integer user, Integer amount)
        throws OversellException {
        RLock lock = redissonClient.getLock(buildLockKey(productId));
        lock.lock();
        try {
            // 先查询库存是否足够
            Response response = restClient.put().uri(
                            "http://inventory-service/inventory?productId={productId}&diff={amount}", productId, amount)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Response.class);
            log.info(response.toString());
            if (!(Boolean) response.getData()) {
                throw new OversellException("商品 " + productId + " 已经超卖了");
            }
            Product product = productMapper.queryProductById(productId);

            Response response2 = restClient.post().uri(
                "http://order-service/order/create?product={product}&price={price}&amount={amount}&title={title}",
                            productId, product.getPrice(), amount, product.getName())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Response.class);
            if (response2.isSuccess()) {
                return response2.getData();
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    private String buildLockKey(Integer productId) {
        return "product:" + productId;
    }
}
