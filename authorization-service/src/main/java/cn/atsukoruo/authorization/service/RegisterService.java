package cn.atsukoruo.authorization.service;


import cn.atsukoruo.authorization.entity.User;
import cn.atsukoruo.authorization.exception.DuplicateUserException;
import cn.atsukoruo.authorization.exception.RegMatchException;
import cn.atsukoruo.authorization.repository.UserMapper;
import com.aliyun.oss.*;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.xiaoju.uemc.tinyid.client.utils.TinyId;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
@DS("sharding")
public class RegisterService {
    static final private String BUCKET_NAME = "atsukoruo-oss-image";
    static final private String LOCK_PREFIX = "register";
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final RedissonClient redissonClient;

    private final PlatformTransactionManager transactionManager;

    private final PasswordEncoder passwordEncoder;

    private final OSS ossClient;

    private final static Pattern usernamePattern;
    private final static Pattern passwordPattern;
    private final static Pattern nicknamePattern;


    static {
        String usernameReg = "^[a-zA-Z0-9]{4,16}$";
        String passwordReg = "^[a-zA-Z0-9@#\\$&\\.]{6,16}$";
        String nicknameReg = "^[\\u4e00-\\u9fa5a-zA-Z0-9]{4,16}$";
        usernamePattern = Pattern.compile(usernameReg);
        passwordPattern = Pattern.compile(passwordReg);
        nicknamePattern = Pattern.compile(nicknameReg);
    }

    @Autowired
    public RegisterService(UserMapper userMapper,
                           RedissonClient redissonClient,
                           PlatformTransactionManager platformTransactionManager,
                           PasswordEncoder passwordEncoder,
                           OSS ossClient,
                           TokenService tokenService) {
        this.userMapper = userMapper;
        this.redissonClient = redissonClient;
        this.transactionManager = platformTransactionManager;
        this.passwordEncoder = passwordEncoder;
        this.ossClient = ossClient;
        this.tokenService = tokenService;
    }

    @Value("${tinyid.user}")
    private String tinyIdUser;

    public User register(String username, String nickname, String password, MultipartFile file)
            throws DuplicateUserException, RegMatchException, IOException {
        RLock lock =  redissonClient.getLock(LOCK_PREFIX + username);
        lock.lock();
        String imgUrl = null;
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            if (userMapper.doesExistUser(username) == 1) {
                // 已经有用户存在了
                throw new DuplicateUserException("用户 " + username + " 已经存在了");
            }
            if (!usernamePattern.matcher(username).matches()) {
                throw new RegMatchException(username + " 用户名不符合规则");
            }
            if (!passwordPattern.matcher(password).matches()) {
                throw new RegMatchException(password + " 密码不符合规则");
            }
            if (!nicknamePattern.matcher(nickname).matches()) {
                throw new RegMatchException(nickname + " 昵称不符合规则");
            }

            imgUrl = uploadPicture(file);
            User user = buildUser(username, nickname, password, imgUrl);
            userMapper.insertUser(user);
            tokenService.insertBatch(user.getId(), 1);
            transactionManager.commit(status);
        }  catch (Exception e) {
            if (!(e instanceof ClientException || e instanceof OSSException) && imgUrl != null) {
                ossClient.deleteObject(BUCKET_NAME, imgUrl);
            }
            transactionManager.rollback(status);
            throw e;
        } finally {
            lock.unlock();
        }
        return null;
    }

    private String uploadPicture(MultipartFile file) throws IOException {
        if (file == null) {
            return "default.jpg";
        }
        InputStream imageStream = file.getInputStream();
        String filename = generateRandomImageFilename(Objects.requireNonNull(file.getOriginalFilename()));
        ossClient.putObject(BUCKET_NAME, filename , imageStream);
        return filename;
    }

    private String generateRandomImageFilename(String originFilename) {
        int index = originFilename.lastIndexOf('.');
        String ext = originFilename.substring(index);
        return UUID.randomUUID() + ext;
    }

    private User buildUser(String username, String nickname, String password, String imgUrl) {
        return User.builder()
                .id(TinyId.nextId(tinyIdUser).intValue())
                .username(username)
                .nickname(nickname)
                .password(passwordEncoder.encode("{bcrypt}"+password))
                .role("user")
                .isBanned(false)
                .isInfluencer(false)
                .avatar_url(imgUrl)
                .createTime(new Timestamp(System.currentTimeMillis()))
                .build();
    }
}
