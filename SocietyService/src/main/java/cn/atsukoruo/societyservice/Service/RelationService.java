package cn.atsukoruo.societyservice.Service;

import cn.atsukoruo.common.entity.User;
import cn.atsukoruo.common.exception.BlacklistError;
import cn.atsukoruo.societyservice.Config.RelationConfig;
import cn.atsukoruo.societyservice.Repository.RelationMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.List;

@Service
public class RelationService {
    private final RelationMapper relationMapper;
    private final RedissonClient redissonClient;
    private final PlatformTransactionManager transactionManager;

    private final UserService userService;
    private final PostService postService;


    public RelationService(RelationMapper relationMapper,
                           RedissonClient redissonClient,
                           PlatformTransactionManager transactionManager,
                           UserService userService,
                           PostService postService) {
        this.relationMapper = relationMapper;
        this.redissonClient = redissonClient;
        this.transactionManager = transactionManager;
        this.postService = postService;
        this.userService = userService;
    }

    public void follow(int followerId, int favoriteUserId) throws BlacklistError {
        RLock lock =  redissonClient.getLock(buildKey(followerId));
        lock.lock();
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            Integer relation = relationMapper.getRelation(followerId, favoriteUserId);
            switch (relation) {
                case null -> {
                    // 和对方暂无关系
                    relationMapper.createRelation(followerId, favoriteUserId, RelationConfig.FOLLOWING);
                    relationMapper.createRelation(favoriteUserId, followerId, RelationConfig.FOLLOWED);
                }
                case RelationConfig.BLACKLISTED ->
                    // 对方已经拉黑你
                        throw new BlacklistError();
                case RelationConfig.FOLLOWED -> {
                    // 对方关注了你
                    relationMapper.updateRelation(followerId, favoriteUserId, RelationConfig.BI_FOLLOWING);
                    relationMapper.updateRelation(favoriteUserId, followerId, RelationConfig.BI_FOLLOWING);
                }
                default -> {
                    // 如果你拉黑了对方，或者你已经关注了对方，那么就直接退出
                    return;
                }
            }
            if (!userService.isInfluencer(favoriteUserId)) {
                // 如果对方不是大 V，那么就将帖子同步推送过来
                postService.copyToInbox(favoriteUserId, followerId);
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
          throw e;
        } finally {
            lock.unlock();
        }
    }


    public void unfollow(int user, int unfollowedUser) {
        RLock lock = redissonClient.getLock(buildKey(user));
        lock.lock();
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            Integer relation = relationMapper.getRelation(user, unfollowedUser);
            if (relation != null
                && (relation == RelationConfig.FOLLOWING || relation == RelationConfig.BI_FOLLOWING)) {
                // 在 FOLLOWING 、BI_FOLLOWING 关系下，才允许进行取关操作
                if (relation == RelationConfig.FOLLOWING) {
                    relationMapper.removeRelation(user, unfollowedUser);
                    relationMapper.removeRelation(unfollowedUser, user);
                } else {
                    relationMapper.updateRelation(user,unfollowedUser, RelationConfig.FOLLOWED);
                    relationMapper.updateRelation(unfollowedUser, user, RelationConfig.FOLLOWING);
                }
                if (!userService.isInfluencer(unfollowedUser)) {
                    // 对方不是大 V，那么就从自己的收件箱中删除有关他的任何帖子
                    postService.deletePostFromInbox(user, unfollowedUser);
                }
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    public void blacklist(Integer user, Integer blacklistedUser) {
        RLock lock = redissonClient.getLock(buildKey(user));
        lock.lock();
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            Integer relation = relationMapper.getRelation(user, blacklistedUser);
            switch (relation) {
                case null:
                    relationMapper.createRelation(user, blacklistedUser, RelationConfig.BLACKLISTING);
                    relationMapper.createRelation(blacklistedUser, user, RelationConfig.BLACKLISTED);
                    break;
                case RelationConfig.BLACKLISTED:
                    // 已经被拉黑了
                    relationMapper.updateRelation(user, blacklistedUser, RelationConfig.BI_BLACKLISTING);
                    relationMapper.updateRelation(blacklistedUser, user, RelationConfig.BI_BLACKLISTING);
                    break;
                case RelationConfig.BI_FOLLOWING, RelationConfig.FOLLOWING, RelationConfig.FOLLOWED:
                    // 当处于关注关系时，那么自动解除该关系，并设置为拉黑关系
                    relationMapper.updateRelation(user, blacklistedUser, RelationConfig.BLACKLISTING);
                    relationMapper.updateRelation(blacklistedUser, user, RelationConfig.BLACKLISTED);
                    if (!userService.isInfluencer(blacklistedUser)) {
                        // 对方不是大 V，那么就从自己的收件箱中删除有关他的任何帖子
                        postService.deletePostFromInbox(user, blacklistedUser);
                    }
                    break;
                case RelationConfig.BLACKLISTING:
                    // 已经拉黑了人家，那么什么也不做
                    // 触发防御性编程语句
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + relation);
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    public void unblacklist(Integer user, Integer blacklistedUser) {
        RLock lock = redissonClient.getLock(buildKey(user));
        lock.lock();
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            Integer relation =  relationMapper.getRelation(user, blacklistedUser);
            if (relation != null
                && (relation == RelationConfig.BI_BLACKLISTING || relation == RelationConfig.BLACKLISTING)) {
                if (relation == RelationConfig.BI_BLACKLISTING) {
                    relationMapper.updateRelation(user, blacklistedUser, RelationConfig.BLACKLISTED);
                    relationMapper.updateRelation(blacklistedUser, user, RelationConfig.BLACKLISTING);
                } else {
                    relationMapper.removeRelation(user, blacklistedUser);
                    relationMapper.removeRelation(blacklistedUser, user);
                }
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        } finally {
            lock.unlock();
        }
    }


    public int getFollowedUserNum(int user) {
        return 0;
    }

    public int getFollowingUserNum(int user) {
        return 0;
    }

    public List<User> getFollowedUser(int user) {
        return null;
    }

    public List<User> getFollowingUser(int user) {
        return null;
    }

    private String buildKey(int id) {
        return "relation:" + id;
    }
}
