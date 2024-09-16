package cn.atsukoruo.societyservice.Service;

import cn.atsukoruo.common.utils.JsonUtils;
import cn.atsukoruo.societyservice.Entity.Post;
import cn.atsukoruo.societyservice.Entity.PostIndex;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final RedissonClient redissonClient;
    private final UserProxyService userProxyService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RelationService relationService;
    private final Executor executor;
    private final OSS ossClient;
    static final private String BUCKET_NAME = "atsukoruo-oss-image";

    public PostService(PostMapper postMapper,
                       RedissonClient redissonClient,
                       OSS ossClient,
                       UserProxyService userProxyService,
                       KafkaTemplate<String, String> kafkaTemplate,
                       @Lazy RelationService relationService,
                       @Qualifier("message-thread-pool") Executor executor) {
        this.postMapper = postMapper;
        this.redissonClient = redissonClient;
        this.ossClient = ossClient;
        this.userProxyService = userProxyService;
        this.kafkaTemplate = kafkaTemplate;
        this.relationService = relationService;
        this.executor = executor;
    }


    /**
     * 将 outbox index 复制到 Inbox 中
     * 副作用：尝试向 inbox cache 添加该索引
     */
    @Transactional
    public void copyAllPostIndexToInbox(int outbox, int inbox) {
        List<PostIndex> indexes = postMapper.getAllValidPostIndexFromInbox(
                outbox,
                new Timestamp(Long.MAX_VALUE),
                Integer.MAX_VALUE);
        postMapper.insertAllPostIndexToInbox(inbox, indexes);
        RScoredSortedSet<Integer> set = redissonClient.getScoredSortedSet(buildInboxCacheKey(inbox));
        for (var index : indexes) {
            addPostIndexToCache(set, inboxMaxSize, index);
        }
    }

    /**
     * 从 userId 的 inbox，移除 removedUser 所有的帖子
     * 副作用：删除 inbox Cache
     */
    public void deletePostIndexInInbox(int user, int removedUser) {
        RScoredSortedSet<Integer> set = redissonClient.getScoredSortedSet(buildInboxCacheKey(user));
        set.delete();
        postMapper.deletePostIndexInInbox(user, removedUser);
    }

    /**
     * 从 outbox 中获取 user 发布的帖子，按照时间排序
     */
    public List<Post> retrievePost(int user, long timestamp, int size) {
        return postMapper.retrievePostFromUser(user, new Timestamp(timestamp), size);
    }


    @Value("${post.outbox.maxSize}")
    private int outboxMaxSize;
    @Value("${post.inbox.maxSize}")
    private int inboxMaxSize;

    private void addPostIndexToInboxCache(int user, PostIndex index) {
        RScoredSortedSet<Integer> set = redissonClient.getScoredSortedSet(buildInboxCacheKey(user));
        addPostIndexToCache(set, inboxMaxSize, index);
    }

    private void addPostIndexToCache(RScoredSortedSet<Integer> set, int maxSize, PostIndex index) {
        if (!set.isExists())
            return;
        double score = Double.longBitsToDouble(index.getCreateTime().getTime());
        // 优先填满缓存
        if (set.size() < maxSize) {
            set.add(score, index.getId());
            return;
        }
        double firstScore = set.firstScore();
        if (firstScore >= score) {
            return;
        }
        set.add(score, index.getId());
        set.pollFirst();
    }

    private void addPostIndexToOutboxCache(int user, PostIndex index) {
        RScoredSortedSet<Integer> set = redissonClient.getScoredSortedSet(buildOutboxCacheKey(user));
        addPostIndexToCache(set, outboxMaxSize, index);
    }

    /**
     * 从 userId 的 outbox index 中，软删除 postId 的帖子
     * 副作用：尝试从 outbox Cache 中删除该帖子
     */
    @Transactional
    public void deletePost(int user, int post) {
        postMapper.deletePostIndexSoftly(user, post);
        String outboxKey = buildOutboxCacheKey(user);
        RScoredSortedSet<Integer> set = redissonClient.getScoredSortedSet(outboxKey);
        if (set.isExists())
            set.remove(post);
    }

    /**
     * 向 outbox 、 outbox index、outbox cache 更新缓存
     */
    @Transactional
    public void publishPost(int user, String content, MultipartFile file) throws IOException {
        String imgUrl = null;
        try {
            imgUrl = uploadPicture(file);
            PostIndex index = buildPostIndex(user);
            Post post =  buildPost(index.getId(), content, imgUrl);
            long timestamp = index.getCreateTime().getTime();
            int postId = index.getId();
            postMapper.insertPostIndex(index);
            post.setPostId(postId);
            postMapper.insertPost(post);
            if (!userProxyService.isInfluencer(user)) {
                // 对于普通用户，要异步推送到各个关注者的 inbox 缓存中
                asyncPublishPostIndexToFollowedUser(user, postId, timestamp);
            }
            addPostIndexToOutboxCache(user, index);
        } catch (Exception e) {
            if (!(e instanceof ClientException || e instanceof OSSException) && imgUrl != null) {
                ossClient.deleteObject(BUCKET_NAME, imgUrl);
            }
            throw e;
        }
    }

    /**
     * 向发件人的关注者的 inbox 推送 postIndex
     */
    private void asyncPublishPostIndexToFollowedUser(int user, int postId, long timestamp) {
        Map<String, Object> map = Map.of("user", user, "postId", postId, "timestamp", timestamp);
        String value = JsonUtils.toJson(map);
        String key = String.valueOf(user);
        ProducerRecord<String, String> record = new ProducerRecord<>("post", key, value);
        kafkaTemplate.send(record);
    }

    /**
     * 向发件人的关注者的 inbox 添加该 postIndex
     * 副作用，尝试向 inbox cache 添加 post
     */
    @Transactional
    public void syncPublishPostIndexToFollowedUser(int user, int postId, long timestamp) {
        List<Integer> users = relationService.getFollowedUser(user, 0, Integer.MAX_VALUE);
        postMapper.publishPostToFollowedUser(user, postId, timestamp, users);
        PostIndex index = PostIndex.builder().userId(user)
                .createTime(new Timestamp(timestamp)).id(postId).build();
        for (var followedUser : users) {
            addPostIndexToInboxCache(followedUser, index);
        }
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

    private Post buildPost(int post, String content, String imgUrl) {
        return Post.builder()
                .postId(post)
                .content(content)
                .imgUrl(imgUrl)
                .build();
    }

    private PostIndex buildPostIndex(int user) {
        return PostIndex.builder()
                .createTime(new Timestamp(System.currentTimeMillis()))
                .userId(user)
                .isDeleted(false)
                .build();
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
        List<PostIndex> res = new ArrayList<>();
        AtomicInteger hasMoreNum = new AtomicInteger(followingUsers.size());
        AtomicInteger total = new AtomicInteger(0);
        AtomicLong lastTimestamp = new AtomicLong(timestamp);


        while(total.get() < size) {
            if (hasMoreNum.get() == 0) {
                res.add(PostIndex.builder().id(-1).build());
                break;
            }
            List<CompletableFuture<List<Post>>> futureList = new ArrayList<>();
            List<PostIndex> posts = new ArrayList<>();

            // 处理自己的帖子
            if (hasMoreForMe.get()) {
                CompletableFuture<List<Post>> myPost = CompletableFuture
                        .supplyAsync(() -> retrievePostIndexFromOutbox(user, timestamp, size), executor)
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
                        .supplyAsync(() -> retrievePostIndexFromInbox(user, lastTimestamp.get(), size), executor)
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
                        .supplyAsync(() -> retrievePostIndexFromInbox(influencer, lastTimestamp.get(), size))
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
            posts.sort(Comparator.comparing(PostIndex::getCreateTime).reversed());
            res.addAll(posts);
            lastTimestamp.set(posts.get(posts.size() - 1).getCreateTime().getTime());
        }
        List<Integer> indexList = res.stream().limit(size).map(PostIndex::getId).toList();
        return postMapper.retrievePostByIds(indexList);
    }

    /**
     *  获取 user 在 inbox 中的帖子 index，按照时间排序
     */
    public List<PostIndex> retrievePostIndexFromInbox(int user, long timestamp, int size) {
        long lastTimestamp = timestamp;
        RScoredSortedSet<Integer> set =  redissonClient.getScoredSortedSet(buildInboxCacheKey(user));
        List<PostIndex> results = new ArrayList<>();

        // 重建缓存
        if (!set.isExists()) {
            RLock lock = redissonClient.getLock(buildInboxCacheLockKey(user));
            try {
                lock.lock();
                // 防止同时构建
                if (!set.isExists())
                    rebuildInboxCache(user);
            } finally {
                lock.unlock();
            }
        }

        double score = Double.longBitsToDouble(lastTimestamp);
        // 先从缓存从获取
        if (score >= set.firstScore()) {
            List<PostIndex> indexes = set
                    .entryRange(0, false, score, true)
                    .stream().map(entry -> PostIndex.builder()
                            .createTime(new Timestamp(Double.doubleToLongBits(entry.getScore())))
                            .id(entry.getValue())
                            .build()).toList();

            results.addAll(indexes);
            PostIndex lastPostIndex = indexes.get(indexes.size() - 1);
            if (lastPostIndex.getId() == -1) {
                return results;
            }
            size -= indexes.size();
            lastTimestamp = lastPostIndex.getCreateTime().getTime();
        }

        // 再从 inbox 中获取
        if (size > 0) {
            List<PostIndex> posts = postMapper.getAllValidPostIndexFromInbox(user, new Timestamp(lastTimestamp), size);
            results.addAll(posts);
        }
        return results;
    }

    private void rebuildInboxCache(int user) {
        List<PostIndex> indexes = postMapper.getAllValidPostIndexFromInbox(user, new Timestamp(Long.MAX_VALUE), inboxMaxSize);
        for (var index : indexes) {
            addPostIndexToInboxCache(user, index);
        }
        if (indexes.size() < inboxMaxSize) {
            // 添加一个特殊的帖子，避免缓存击穿
            PostIndex nullIndex = PostIndex.builder().id(-1).build();
            addPostIndexToInboxCache(user, nullIndex);
        }
    }


    /**
     * 获取 user 在 outbox 中的帖子 index，按照时间排序
     */
    public List<PostIndex> retrievePostIndexFromOutbox(int user, long timestamp, int size) {
        long lastTimestamp = timestamp;
        RScoredSortedSet<Integer> set =  redissonClient.getScoredSortedSet(buildOutboxCacheKey(user));
        List<PostIndex> results = new ArrayList<>();

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

        double score = Double.longBitsToDouble(lastTimestamp);
        // 先从缓存从获取
        if (lastTimestamp >= set.firstScore()) {
            // outbox 中不存储删除过的 post
            List<PostIndex> indexes = set
                    .entryRange(0, false, score, true)
                    .stream().map(entry -> PostIndex.builder()
                            .createTime(new Timestamp(Double.doubleToLongBits(entry.getScore())))
                            .id(entry.getValue())
                            .build()).toList();
            results.addAll(indexes);
            PostIndex lastPostIndex = indexes.get(indexes.size() - 1);
            if (lastPostIndex.getId() == -1) {
                return results;
            }
            size -= indexes.size();
            lastTimestamp = lastPostIndex.getCreateTime().getTime();
        }

        // 再从 outbox 中获取
        if (size > 0) {
            List<PostIndex> posts = postMapper.getAllValidPostIndexFromOutbox(user, new Timestamp(lastTimestamp), size);
            results.addAll(posts);
        }
        return results;
    }

    private void rebuildOutboxCache(int user) {
        List<PostIndex> indexes = postMapper.getAllValidPostIndexFromOutbox(user, new Timestamp(Long.MAX_VALUE), outboxMaxSize);
        for (var index : indexes) {
            addPostIndexToOutboxCache(user, index);
        }
        if (indexes.size() < outboxMaxSize) {
            // 添加一个特殊的帖子，避免缓存击穿
            PostIndex nullIndex = PostIndex.builder().id(-1).build();
            addPostIndexToOutboxCache(user, nullIndex);
        }
    }

    private String buildOutboxCacheKey(int user) {
        return "ob" + user;
    }

    private String buildOutboxCacheLockKey(int user) {
        return "obl" + user;
    }
    private String buildInboxCacheKey(int user) {return "ib" + user; }
    private String buildInboxCacheLockKey(int user) { return "ibl" + user; }
}
