package cn.atsukoruo.societyservice.Service;

import cn.atsukoruo.societyservice.Entity.Post;
import cn.atsukoruo.societyservice.Repository.PostMapper;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class PostService {
    private final PostMapper postMapper;
    private final PlatformTransactionManager transactionManager;

    private final RedissonClient redissonClient;

    static final private String BUCKET_NAME = "atsukoruo-oss-image";

    private final OSS ossClient;

    public PostService(PostMapper postMapper,
                       PlatformTransactionManager transactionManager,
                       RedissonClient redissonClient,
                       OSS ossClient) {
        this.postMapper = postMapper;
        this.transactionManager = transactionManager;
        this.redissonClient = redissonClient;
        this.ossClient = ossClient;
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
            buildPost(user, content, imgUrl);
            redissonClient.getScoredSortedSet("");
        } catch (Exception e) {
            if (!(e instanceof ClientException || e instanceof OSSException) && imgUrl != null) {
                ossClient.deleteObject(BUCKET_NAME, imgUrl);
            }
            transactionManager.rollback(status);
            throw e;
        } finally {
            transactionManager.commit(status);
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

    private Post buildPost(int user, String content, String imgUrl) {
        return Post.builder()
                .id(0)
                .content(content)
                .imgUrl(imgUrl)
                .creatTime(new Date(System.currentTimeMillis()))
                .isDeleted(false)
                .userId(user).build();
    }
}
