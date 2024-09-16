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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


@Service
public class PostService {
    private final PostMapper postMapper;
    private final PlatformTransactionManager transactionManager;
    private final RedissonClient redissonClient;
    private final UserProxyService userProxyService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RelationService relationService;
    private final Executor executor;
    private final OSS ossClient;
    static final private String BUCKET_NAME = "atsukoruo-oss-image";

    public PostService(PostMapper postMapper,
                       PlatformTransactionManager transactionManager,
                       RedissonClient redissonClient,
                       OSS ossClient,
                       UserProxyService userProxyService,
                       KafkaTemplate<String, String> kafkaTemplate,
                       @Lazy RelationService relationService,
                       @Qualifier("post-thread-pool") Executor executor) {
        this.postMapper = postMapper;
        this.transactionManager = transactionManager;
        this.redissonClient = redissonClient;
        this.ossClient = ossClient;
        this.userProxyService = userProxyService;
        this.kafkaTemplate = kafkaTemplate;
        this.relationService = relationService;
        this.executor = executor;
    }


    /**
     * 将 outbox 所指定的用户的发件箱中的所有帖子，发送到 inbox 所指定用户的收件箱中
     */
    public void copyAllPostsToInbox(int outbox, int inbox) {
        List<Post> posts = postMapper.getPostInfoOfUser(outbox);
        postMapper.copyAllPostsToInbox(outbox, inbox, posts);
    }

    /**
     * 从 userId 的收件箱中，移除 removedUser 所有的帖子
     */
    public void deletePostFromUserInInbox(int user, int removedUser) {
        postMapper.deletePostFromUserInInbox(user, removedUser);
    }

    /**
     * 直接从数据库中获取 user 发布的帖子，按照时间排序，获取 [from, from + size] 个
     */
    public List<Post> retrievePost(int user, long timestamp, int size) {
        return postMapper.retrievePost(user, new Timestamp(timestamp), size, false);
    }


    @Value("${post.outbox.maxSize}")
    private int outboxMaxSize;


    private void addPostToOutboxCache(int user, Post post) {
        RScoredSortedSet<Integer> set = redissonClient.getScoredSortedSet(buildOutboxCacheKey(user));
        if (!set.isExists())
            return;
        set.add(Double.longBitsToDouble(post.getCreateTime().getTime()),
                post.getId());
        while (set.size() > outboxMaxSize) {
            // 删除分数最小的成员
            set.pollFirst();
        }
    }

    /**
     * 从 userId 的发件箱中，软删除 postId 的帖子，并删除 outbox 缓存（有的话）中的对应帖子
     */
    public void deletePostSoftlyInOutbox(int user, int post) {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            postMapper.deletePostSoftlyInOutbox(user, post);
            String outboxKey = buildOutboxCacheKey(user);
            RScoredSortedSet<Integer> set = redissonClient.getScoredSortedSet(outboxKey);
            if (set.isExists())
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
                // 对于普通用户，还要异步推送到各个关注者的 inbox 缓存中
                asyncPublishPostToFollowedUser(user, postId, timestamp);
            }

            // 对于大 V 用户，直接写入到 outbox 缓存中即可
            // 普通用户也将帖子写入到 outbox 缓存中
            addPostToOutboxCache(user, post);
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
     * 向发件人的关注者的 inbox 推送帖子（普通用户）
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
     * 向发件人的关注者（不包括自己）的 inbox 添加该帖子
     * @param user      发件人
     * @param postId    帖子的 ID
     * @param timestamp  帖子的创建时间
     */
    public void syncPublishPostToFollowedUser(int user, int postId, long timestamp) {
        List<Integer> users = relationService.getFollowedUser(user, 0, Integer.MAX_VALUE);
        postMapper.publishPostToFollowedUser(user, postId, timestamp, users);
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

    private String buildOutboxCacheKey(int user) {
        return "ob" + user;
    }

    private String buildOutboxCacheLockKey(int user) {
        return "obl" + user;
    }

    public List<Post> feed(int user, long timestamp, int size) {
        // 获取关注者列表
        List<Integer> followingUsers =  relationService.getFollowingUser(user, 0, Integer.MAX_VALUE);

        // 区分出大 V 用户
        List<Integer> influencerUsers = new ArrayList<>();
        HashMap<Integer, Boolean> hasMoreForInfluencer = new HashMap<>();
        followingUsers.stream().filter(userProxyService::isInfluencer).forEach(influencer -> {
            hasMoreForInfluencer.put(influencer, true);
            influencerUsers.add(influencer);
        });


        AtomicBoolean hasMoreForMe = new AtomicBoolean(true);
        AtomicBoolean hasMoreForNormalUser = new AtomicBoolean(true);
        List<Post> res = new ArrayList<>();
        AtomicInteger hasMoreNum = new AtomicInteger(followingUsers.size());
        AtomicInteger total = new AtomicInteger(0);
        AtomicLong lastTimestamp = new AtomicLong(timestamp);


        while(total.get() < size) {
            if (hasMoreNum.get() == 0) {
                res.add(Post.builder().id(-1).build());
                break;
            }
            List<CompletableFuture<List<Post>>> futureList = new ArrayList<>();
            List<Post> posts = new ArrayList<>();

            // 处理自己的帖子
            if (hasMoreForMe.get()) {
                CompletableFuture<List<Post>> myPost = CompletableFuture
                        .supplyAsync(() -> retrievePostFromOutbox(user, timestamp, size), executor)
                        .thenApply(results -> {
                            if (results.size() < size) {
                                // 没有更多的帖子了
                                hasMoreForMe.set(false);
                            }
                            hasMoreNum.decrementAndGet();
                            total.addAndGet(results.size());
                            posts.addAll(results);
                            return null;
                        });
                futureList.add(myPost);
            }


            // 处理普通用户的帖子
            if (hasMoreForNormalUser.get()) {
                CompletableFuture<List<Post>> future = CompletableFuture
                        .supplyAsync(() -> retrievePostFromInbox(user, lastTimestamp.get(), size), executor)
                        .thenApply(results -> {
                            int num = results.size();
                            if (num < size) {
                                hasMoreForNormalUser.set(false);
                            }
                            hasMoreNum.decrementAndGet();
                            total.addAndGet(num);
                            posts.addAll(results);
                            return null;
                        });
                futureList.add(future);
            }

            // 处理大 V 用户的帖子
            for (int influencer : influencerUsers) {
                if (!hasMoreForInfluencer.get(influencer))
                    continue;
                CompletableFuture<List<Post>> future = CompletableFuture
                        .supplyAsync(() -> retrievePostFromOutbox(influencer, lastTimestamp.get(), size))
                        .thenApply(results -> {
                            int num = results.size();
                            if (num < size) {
                                hasMoreForInfluencer.put(influencer, false);
                            }
                            hasMoreNum.decrementAndGet();
                            total.addAndGet(num);
                            posts.addAll(results);
                            return null;
                        });
                futureList.add(future);
            }

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
            allFutures.join();
            // 进行排序
            posts.sort(Comparator.comparing(Post::getCreateTime).reversed());
            res.addAll(posts);
            lastTimestamp.set(posts.get(posts.size() - 1).getCreateTime().getTime());
        }
        return res.stream().limit(size).toList();
    }

    /**
     *  获取 user 在 inbox 中的帖子，按照时间排序，获取 [from, from + size] 个
     */
    public List<Post> retrievePostFromInbox(int user, long timestamp, int size) {
        List<Post> results = new ArrayList<>();
        boolean terminated = false;
        while (size > 0 && !terminated) {
            // 保证从 inbox 中获取到的帖子所属的用户，目前都是在关注列表中的。
            List<Integer> ids = postMapper.getPostIdsFromInbox(user, new Timestamp(timestamp), size);
            if (ids.size() < size) {
                terminated = true;
            }
            List<Post> posts = postMapper.selectAllPostByIds(ids, false);
            results.addAll(posts);
            size -= posts.size();
        }
        return results;
    }



    /**
     * 获取 user 发布的帖子，按照时间排序，获取 [from, from + size] 个
     * 其中涉及到缓存
     */
    public List<Post> retrievePostFromOutbox(int user, long timestamp, int size) {
        long lastTimestamp = timestamp;
        RScoredSortedSet<Integer> set =  redissonClient.getScoredSortedSet(buildOutboxCacheKey(user));
        List<Post> results = new ArrayList<>();

        // 重建缓存
        if (!set.isExists()) {
            RLock lock = redissonClient.getLock(buildOutboxCacheLockKey(user));
            try {
                lock.lock();
                // 防止同时构建
                if (!set.isExists())
                    rebuildOutboxCache(user);
            } finally {
                lock.unlock();
            }
        }

        // 先从缓存从获取
        if (lastTimestamp >= set.first()) {
            // outbox 中不存储删除过的 post
            List<Integer> ids = set
                    .valueRangeReversed(0, false, lastTimestamp, true)
                    .stream().toList();
            List<Post> posts =  postMapper.selectAllPostByIds(ids, false);
            results.addAll(posts);
            Post lastPost = posts.get(posts.size() - 1);
            if (lastPost.getId() == -1) {
                return results;
            }
            size -= posts.size();
            lastTimestamp = lastPost.getCreateTime().getTime();
        }

        // 再从 outbox 中获取
        if (size > 0) {
            List<Post> posts = postMapper.retrievePost(user, new Timestamp(lastTimestamp), size, false);
            results.addAll(posts);
        }
        return results;
    }

    private void rebuildOutboxCache(int user) {
        List<Post> posts = postMapper.getPostInfoOfUser(user, new Timestamp(Long.MAX_VALUE), outboxMaxSize);
        for (var post : posts) {
            addPostToOutboxCache(user, post);
        }
        if (posts.size() < outboxMaxSize) {
            // 添加一个特殊的帖子，避免缓存击穿
            addPostToOutboxCache(
                    user,
                    Post.builder().id(-1).createTime(new Timestamp(0)).build());
        }
    }

}
