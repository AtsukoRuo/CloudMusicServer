package cn.atsukoruo.societyservice.Service;

import cn.atsukoruo.common.utils.JsonUtils;
import cn.atsukoruo.societyservice.Entity.Post;
import cn.atsukoruo.societyservice.Repository.PostMapper;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.redisson.api.RLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class PostService {
    private final PostMapper postMapper;
    private final PlatformTransactionManager transactionManager;

    private final RedissonClient redissonClient;

    private final UserProxyService userProxyService;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RelationService relationService;

    static final private String BUCKET_NAME = "atsukoruo-oss-image";



    private final OSS ossClient;

    public PostService(PostMapper postMapper,
                       PlatformTransactionManager transactionManager,
                       RedissonClient redissonClient,
                       OSS ossClient,
                       UserProxyService userProxyService,
                       KafkaTemplate<String, String> kafkaTemplate,
                       @Lazy RelationService relationService) {
        this.postMapper = postMapper;
        this.transactionManager = transactionManager;
        this.redissonClient = redissonClient;
        this.ossClient = ossClient;
        this.userProxyService = userProxyService;
        this.kafkaTemplate = kafkaTemplate;
        this.relationService = relationService;
    }

    public void copyToInbox(int outbox, int inbox) {
        List<Integer> ids = postMapper.getPostIdsOfUser(outbox);
        if (ids.size() != 0) {
            postMapper.insertToInbox(outbox, inbox, ids);
        }
    }

    public void deletePostFromInbox(int userId, int removedUserId) {
        postMapper.deletePostInInbox(userId, removedUserId);
    }

    public void publishPost(int user, String content, MultipartFile file) throws IOException {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        String imgUrl = null;
        try {
            imgUrl = uploadPicture(file);
            Post post =  buildPost(user, content, imgUrl);
            Integer postId =  postMapper.insertPost(post);
            if (!userProxyService.isInfluencer(user)) {
                asyncPublishPostToFollowedUser(user, postId);
            } else {
                deleteOutBoxCache(user);
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            if (!(e instanceof ClientException || e instanceof OSSException) && imgUrl != null) {
                ossClient.deleteObject(BUCKET_NAME, imgUrl);
            }
            transactionManager.rollback(status);
            throw e;
        }
    }


    private void asyncPublishPostToFollowedUser(int user, int postId) {
        Map<String, Object> map = Map.of("user", user, "postId", postId);
        String value = JsonUtils.toJson(map);
        String key = String.valueOf(user);
        ProducerRecord<String, String> record = new ProducerRecord<>("post", key, value);
        kafkaTemplate.send(record);
    }

    public void syncPublishPostToFollowedUser(int user, int postId) {
        List<Integer> users = relationService.getFollowedUser(user, 0, Integer.MAX_VALUE);
        postMapper.insertToInbox(user, postId, users);
    }


    @Value("${post.outbox.maxSize}")
    private int outboxMaxSize;

    @Value("${post.outbox.maxDay}")
    private int outboxMaxDay;

    private void rebuildOutboxCache(int user, int maxSize, int day) {
        RScoredSortedSet<String> set =  redissonClient.getScoredSortedSet(buildOutboxKey(user));
        if (set == null) {
            RLock lock =  redissonClient.getLock(buildOutboxLockKey(user));
            lock.lock();
            try {
                if (set == null) {
                    rebuildOutboxCache(user, outboxMaxSize, outboxMaxSize);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void deleteOutBoxCache(int user) {
        RLock lock = redissonClient.getLock(buildInboxLockKey(user));
        lock.lock();
        try {
            redissonClient.getScoredSortedSet(buildOutboxKey(user)).delete();
        } finally {
            lock.unlock();
        }
    }
    private void rebuildInboxCache(int user, int maxSize, int day) {

    }


    private String uploadPicture(MultipartFile file) throws IOException {
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

    private Post buildPost(int user, String content, String imgUrl) {
        return Post.builder()
                .id(0)
                .content(content)
                .imgUrl(imgUrl)
                .createTime(new Timestamp(System.currentTimeMillis()))
                .isDeleted(false)
                .userId(user).build();
    }


    private String buildOutboxKey(int user) {
        return "ob" + user;
    }

    private String buildInboxKey(int user) {
        return "ib" + user;
    }

    private String buildOutboxLockKey(int user) {
        return "obl" + user;
    }

    private String buildInboxLockKey(int user) {
        return "inl" + user;
    }
}
