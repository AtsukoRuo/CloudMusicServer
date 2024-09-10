package cn.atsukoruo.AuthorizationService.Service;



import cn.atsukoruo.AuthorizationService.Exception.PasswordNotCorrectedException;
import cn.atsukoruo.AuthorizationService.Exception.UserBannedException;
import cn.atsukoruo.AuthorizationService.Exception.UserNotFoundException;
import cn.atsukoruo.AuthorizationService.Repository.UserMapper;
import cn.atsukoruo.common.entity.AccessTokenPayload;
import cn.atsukoruo.common.entity.RefreshTokenPayload;
import cn.atsukoruo.common.entity.User;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Service
public class LoginService {
    static final private String LOCK_PREFIX = "login";
    final private UserMapper userMapper;
    final private PasswordEncoder passwordEncoder;
    final private TokenService tokenService;

    final private PlatformTransactionManager transactionManager;
    final private RedissonClient redisson;


    @Autowired
    public LoginService(UserMapper mapper,
                        PasswordEncoder passwordEncoder,
                        TokenService tokenService,
                        PlatformTransactionManager transactionManager,
                        RedissonClient redisson) {
        this.userMapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.transactionManager= transactionManager;
        this.redisson = redisson;
    }

    /**
     * @return 登录成功后，返回两个 Token，分别是 AccessToken 以及 RefreshToken
     */
    public String[] login(String username, String password, String client)
            throws UserNotFoundException, PasswordNotCorrectedException, UserBannedException {
        RLock lock =  redisson.getLock(LOCK_PREFIX + username);
        lock.lock();
        TransactionStatus status =  transactionManager.getTransaction(new DefaultTransactionDefinition());
        // 实际上，这里的 Redis 事务是一个伪需求，Redis 本身并没有提供 ACID 属性。
        // RTransaction redisTx = redisson.createTransaction(TransactionOptions.defaults());
        try {
            User user = userMapper.selectByUsername(username);
            if (user == null) {
                throw new UserNotFoundException("未查询到用户 " + username);
            }
            if (user.isBanned()) {
                throw new UserBannedException(username + " 已经被封禁");
            }
            if (!passwordEncoder.matches("{bcrypt}" + password, user.getPassword())) {
                throw new PasswordNotCorrectedException(username + " 用户输入了错误的密码 " + password);
            }
            int userID = user.getId();
            // 同设备剔除下线
            tokenService.banAccessTokenForClient(userID, client);
            tokenService.banRefreshTokenForClient(userID, client);

            // 更新并获取最新的 version
            if (tokenService.addVersion(userID, client, 1) == 0) {
                // 说明此设备是第一次登录
                tokenService.insertVersion(userID, client,1);
            }

            int version = tokenService.getVersion(userID, client);
            // 我们在 RegisterController 中，将用户的 batch 添加到了数据库中
            // 因此这里的 Batch 并不会返回 null
            int batch = tokenService.getBatch(userID);


            // 生成 AccessToken 载荷
            AccessTokenPayload accessTokenPayload = AccessTokenPayload.builder()
                    .userId(userID)
                    .role(user.getRole())
                    .client(client)
                    .version(version)
                    .batch(batch)
                    .build();

            // 生成 RefreshToken 载荷
            RefreshTokenPayload refreshTokenPayload = RefreshTokenPayload.builder()
                    .userId(userID)
                    .client(client)
                    .version(version)
                    .batch(batch)
                    .build();

            String accessToken = tokenService.generateAccessToken(accessTokenPayload);
            String refreshToken = tokenService.generateRefreshToken(refreshTokenPayload);
            transactionManager.commit(status);
            return new String[]{accessToken, refreshToken};
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        } finally {
            lock.unlock();
        }
    }
}


