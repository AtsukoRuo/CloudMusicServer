package cn.atsukoruo.AuthorizationService.Service;


import cn.atsukoruo.AuthorizationService.Entity.User;
import cn.atsukoruo.AuthorizationService.Exception.ExpiredJwtException;
import cn.atsukoruo.AuthorizationService.Exception.UserBannedException;
import cn.atsukoruo.AuthorizationService.Exception.UserNotFoundException;
import cn.atsukoruo.AuthorizationService.Repository.TokenMapper;
import cn.atsukoruo.AuthorizationService.Repository.UserMapper;
import cn.atsukoruo.common.config.TokenClaimsConfig;
import cn.atsukoruo.common.entity.AccessTokenPayload;
import cn.atsukoruo.common.entity.RefreshTokenPayload;
import cn.atsukoruo.common.exception.BannedRefreshTokenException;
import com.baomidou.dynamic.datasource.annotation.DS;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Duration;
import java.util.Map;

@Service
@DS("token")
public class TokenService {
     @Value("${jwt.signing-key}")
    private String signingKey;

    @Value("${jwt.expireTimeOfAccessToken}")
    private long expireTimeOfAccessTime;

    @Value("${jwt.expireTimeOfRefreshToken}")
    private long expireTimeOfRefreshTime;

    private final TokenMapper tokenMapper;

    private final RedissonClient redissonClient;

    private final UserMapper userMapper;

    @Autowired
    public TokenService(TokenMapper tokenMapper,
                        RedissonClient redissonClient,
                        UserMapper userMapper) {
        this.tokenMapper = tokenMapper;
        this.redissonClient = redissonClient;
        this.userMapper = userMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertVersion(int userId, String client, int version) {
        tokenMapper.insertVersion(userId, client, version);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertBatch(int userId, int batch) {
        tokenMapper.insertBatch(userId, batch);
    }

    /**
     * 获取版本号 version
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Integer getVersion(int userId, String client) {
        return tokenMapper.selectVersion(userId, client);
    }

    /**
     * 获取批次号
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Integer getBatch(int userID) {
        return tokenMapper.selectBatch(userID);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int addVersion(int userId, String client, int diff) {
        return tokenMapper.addVersion(userId, client, diff);
    }

    int addBatch(int userID, int diff) {
        return tokenMapper.addBatch(userID, diff);
    }


    private String buildAccessVersionKey(int userId, String client) {
        return "access:" + userId + ":" + client;
    }

    private String buildRefreshVersionKey(int userId, String client) {
        return "refresh:" + userId + ":" + client;
    }

    private String buildAccessBatchKey(int userId) {
        return "access:" + userId;
    }

    private String buildRefreshBatchKey(int userId) {
        return "refresh:" + userId;
    }

    /**
     * 会禁用最新 Version 的 AccessToken
     * 记得在调用完该函数后，调用 addVersion 来生成新的 version
     * 当 userID:client 不存在时，那么就直接返回
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void banAccessTokenForClient(int userId, String client) {
        Integer version =  tokenMapper.selectVersion(userId, client);
        if (version == null) {
            return;
        }
        String key = buildAccessVersionKey(userId, client);
        String value = version.toString();
        redissonClient.getBucket(key).set(value, Duration.ofMillis(expireTimeOfAccessTime));
    }

    /**
     * 会禁用最新 Batch 的 AccessToken
     * 记得在调用完该函数后，调用 addBatch 来生成新的 batch
     * 当 userID:client 不存在时，那么就直接返回
     */
    public void banAccessTokenForBatch(int userId) {
        Integer batch = tokenMapper.selectBatch(userId);
        if (batch == null) {
            return;
        }
        String key = buildAccessBatchKey(userId);
        String value = batch.toString();
        redissonClient.getBucket(key).set(value, Duration.ofMillis(expireTimeOfRefreshTime));
    }

    /**
     * 会禁用最新 Version 的 RefreshToken
     * 记得在调用完该函数后，调用 addVersion 来生成新的 version
     * 当 userID:client 不存在时，那么就直接返回
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void banRefreshTokenForClient(int userId, String client) {
        Integer version =  tokenMapper.selectVersion(userId, client);
        if (version == null) {
            return;
        }
        String key = buildRefreshVersionKey(userId, client);
        String value = version.toString();
        redissonClient.getBucket(key).set(value, Duration.ofMillis(expireTimeOfAccessTime));
    }

    /**
     * 会禁用最新 Batch 的 RefreshToken
     * 记得在调用完该函数后，调用 addBatch 来生成新的 Batch
     * 当 userID:client 不存在时，那么就直接返回
     */
    public void banRefreshTokenForBatch(int userId) {
        Integer batch = tokenMapper.selectBatch(userId);
        if (batch == null) {
            return;
        }
        String key = buildRefreshBatchKey(userId);
        String value =  batch.toString();
        redissonClient.getBucket(key).set(value, Duration.ofMillis(expireTimeOfRefreshTime));
    }


    /**
     * 传递一个 payload，无需设置 expireTime 字段
     */
    public String generateAccessToken(AccessTokenPayload payload) {
        long futureTimeInMillis = System.currentTimeMillis() + expireTimeOfAccessTime * 1000;
        Map<String, Object> claims = Map.of(
                TokenClaimsConfig.USER_ID, payload.getUserId(),
                TokenClaimsConfig.ROLE, payload.getRole(),
                TokenClaimsConfig.CLIENT, payload.getClient(),
                TokenClaimsConfig.VERSION, payload.getVersion(),
                TokenClaimsConfig.BATCH, payload.getBatch(),
                TokenClaimsConfig.EXPIRE_TIME, new Date(futureTimeInMillis)
        );
        return generateToken(claims);
    }

    /**
     * 传递一个 payload，无需设置 expireTime 字段
     */
    public String generateRefreshToken(RefreshTokenPayload payload) {
        long futureTimeInMillis = System.currentTimeMillis() + expireTimeOfRefreshTime * 1000;
        Map<String, Object> claims = Map.of(
                TokenClaimsConfig.USER_ID, payload.getUserId(),
                TokenClaimsConfig.CLIENT, payload.getClient(),
                TokenClaimsConfig.VERSION, payload.getVersion(),
                TokenClaimsConfig.BATCH, payload.getBatch(),
                TokenClaimsConfig.EXPIRE_TIME, new Date(futureTimeInMillis)
        );
        return generateToken(claims);
    }


    private String generateToken(Map<String, Object> claims) {
        SecretKey key = Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setClaims(claims)
                .signWith(key)
                .compact();
    }


    public String refreshAccessToken(String refreshToken)
            throws ExpiredJwtException, UserNotFoundException, UserBannedException, BannedRefreshTokenException {
        SecretKey key = Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();

        Integer userId = (Integer)claims.get(TokenClaimsConfig.USER_ID);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UserNotFoundException("未查询到 userID: " + userId);
        }

        RLock lock =  redissonClient.getLock(user.getUsername());
        lock.lock();
        try {
            if (user.isBanned()) {
                throw new UserBannedException("用户已被封禁" + user.getUsername());
            }

            long expiredTime = (long)claims.get(TokenClaimsConfig.EXPIRE_TIME);
            if (System.currentTimeMillis() > expiredTime) {
                throw new ExpiredJwtException("令牌过期时间为 " + expiredTime);
            }

            // 检测 RefreshToken 是否在黑名单中
            String client = (String)claims.get(TokenClaimsConfig.CLIENT);
            String strVersion = (String) redissonClient.getBucket(buildRefreshVersionKey(userId,client)).get();
            String strBatch = (String) redissonClient.getBucket(buildRefreshBatchKey(userId)).get();
            int bannedVersion = strVersion == null ? 0 : Integer.parseInt(strVersion);
            int bannedBatch = strBatch == null ? 0 : Integer.parseInt(strBatch);
            int version = (Integer)claims.get(TokenClaimsConfig.VERSION);
            int batch = (Integer)claims.get(TokenClaimsConfig.BATCH);
            boolean isBanned = batch <= bannedBatch || version <= bannedVersion;


            if (isBanned) {
                throw new BannedRefreshTokenException("用户ID " + userId + " 的 RefreshToken 已被禁用");
            }
            AccessTokenPayload accessTokenPayload = AccessTokenPayload.builder()
                    .userId(userId)
                    .role(user.getRole())
                    .client(client)
                    .version(version)
                    .batch(batch)
                    .build();
            return generateAccessToken(accessTokenPayload);
        } finally {
            lock.unlock();
        }
    }

}
