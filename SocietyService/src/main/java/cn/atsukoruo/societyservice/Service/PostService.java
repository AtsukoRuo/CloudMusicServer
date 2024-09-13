package cn.atsukoruo.societyservice.Service;

import cn.atsukoruo.common.utils.JsonUtils;
import cn.atsukoruo.societyservice.Entity.Post;
import cn.atsukoruo.societyservice.Repository.PostMapper;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
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


    /**
     * 将 outbox 所指定的用户的发件箱中的帖子，发送到 inbox 所指定用户的收件箱中
     */
    public void copyToInbox(int outbox, int inbox) {
        List<Post> posts = postMapper.getPostInfoOfUser(outbox);
        for (Post post : posts) {
            postMapper.insertToInbox(
                    outbox,
                    post.getId(),
                    post.getCreateTime().getTime(),
                    List.of(inbox));
        }
    }

    /**
     * 从 userId 的收件箱中，移除 removedUserId 所有的帖子
     */
    public void deletePostFromUserInInbox(int user, int removedUser) {
        postMapper.deletePostFromUserInInbox(user, removedUser);
    }

    /**
     * 从 userId 的发件箱中，软删除 postId 的帖子
     */
    public void deletePostSoftlyInOutbox(int user, int post) {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            postMapper.deletePostSoftlyInOutbox(user, post);
            String outboxKey = buildOutboxKey(user);
            RScoredSortedSet<Integer> set = redissonClient.getScoredSortedSet(outboxKey);
            if (set.isExists())
                return;
            set.remove(outboxKey);
            transactionManager.commit(status);
        } catch (Exception e){
            transactionManager.rollback(status);
            throw e;
        }
    }

    public void publishPost(int user, String content, MultipartFile file) throws IOException {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        String imgUrl = null;
        try {
            imgUrl = uploadPicture(file);
            Post post =  buildPost(user, content, imgUrl);
            long timestamp = post.getCreateTime().getTime();
            Integer postId =  postMapper.insertPost(post);
            if (!userProxyService.isInfluencer(user)) {
                // 对于普通用户，异步推送到各个关注者的 inbox 缓存中
                asyncPublishPostToFollowedUser(user, postId, timestamp);
            } else {
                // 对于大 V 用户，直接写入到 outbox 缓存中即可
                RScoredSortedSet<Integer> set = redissonClient.getScoredSortedSet(buildOutboxKey(user));
                if (set != null) {
                    set.add(timestamp, postId);
                }
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

    /**
     * 向发件人的关注者的推送帖子
     * 使用 Kafka 做异步推送
     * @param user      发件人
     * @param postId    帖子的 ID
     * @param timestamp  帖子的创建时间
     */
    private void asyncPublishPostToFollowedUser(int user, int postId, long timestamp) {
        Map<String, Object> map = Map.of("user", user, "postId", postId, "timestamp", timestamp);
        String value = JsonUtils.toJson(map);
        String key = String.valueOf(user);
        ProducerRecord<String, String> record = new ProducerRecord<>("post", key, value);
        kafkaTemplate.send(record);
    }

    /**
     * 向发件人的关注者的 inbox 添加该帖子
     * 并尝试向 inbox 缓存中添加该帖子
     * @param user      发件人
     * @param postId    帖子的 ID
     * @param timestamp  帖子的创建时间
     */
    public void syncPublishPostToFollowedUser(int user, int postId, long timestamp) {
        List<Integer> users = relationService.getFollowedUser(user, 0, Integer.MAX_VALUE);
        postMapper.insertToInbox(user, postId, timestamp, users);
        for (int followedUser : users) {
            RScoredSortedSet<Integer> set = redissonClient.getScoredSortedSet(buildOutboxKey(followedUser));
            if (set != null) {
                set.add(timestamp, postId);
            }
        }
    }


    @Value("${post.outbox.maxSize}")
    private int outboxMaxSize;

    @Value("${post.outbox.maxDay}")
    private int outboxMaxDay;

    private void buildOutboxCache(int user, int maxSize, int day) {
        RScoredSortedSet<String> set =  redissonClient.getScoredSortedSet(buildOutboxKey(user));
        if (set == null) {
            RLock lock =  redissonClient.getLock(buildOutboxLockKey(user));
            lock.lock();
            try {
                // 这里考虑到了并发的情况
                if (set != null) {
                    return;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void buildInboxCache(int user, int maxSize, int day) {

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
