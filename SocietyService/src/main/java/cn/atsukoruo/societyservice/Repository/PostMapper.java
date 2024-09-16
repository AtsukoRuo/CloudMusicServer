package cn.atsukoruo.societyservice.Repository;

import cn.atsukoruo.societyservice.Entity.Post;
import cn.atsukoruo.societyservice.Entity.PostIndex;
import org.apache.ibatis.annotations.*;
import org.bouncycastle.util.Times;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Mapper
@Repository
public interface PostMapper {
    /**
     * 将 postIndex 插入到 Inbox 中
     */
    @InsertProvider(type = PostProvider.class, method = "insertAllPostIndexToInbox")
    void insertAllPostIndexToInbox(Integer inbox, List<PostIndex> posts);

    /**
     * @param from      发帖人
     * @param postId    帖子 ID
     * @param ids       向这些用户推送该帖子
     * @param timestamp 帖子的时间戳
     */
    @InsertProvider(type = PostProvider.class, method = "publishPostToFollowedUser")
    void publishPostToFollowedUser(Integer from, Integer postId,  long timestamp, List<Integer> ids);

    @Delete("DELETE FROM inbox WHERE from_id=#{removedUserId} AND user_id=#{userId}")
    void deletePostIndexInInbox(Integer userId, Integer removedUserId);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT post_index(id, create_time, user_id, is_deleted)" +
            "VALUES(#{id}, #{createTime}, #{userId}, #{isDeleted})")
    void insertPostIndex(PostIndex index);

    @Insert("INSERT post(post_id, content, img_url, user_id) VALUES(#{postId}, #{content}, #{imgUrl}, #{userId})")
    void insertPost(Post post);

    @Update("UPDATE `post_index` SET `is_deleted` = 1 WHERE `id` = #{post} AND `user_id`=#{user}")
    void deletePostIndexSoftly(int user, int post);


    /**
     * 获取指定用户的帖子，降序排序
     * 过滤掉删除的帖子
     */
    @Select("SELECT * FROM post WHERE user_id=#{user} AND create_time < #{timestamp} ORDER BY create_time DESC LIMIT 0,#{size}")
    List<Post> retrievePostFromUser(int user, Timestamp timestamp, int size);


    @InsertProvider(type = PostProvider.class, method = "retrievePostByIds")
    List<Post> retrievePostByIds(List<Integer> ids);

    /**
     * 获取所指定用户的 outbox 中所有的帖子索引，但是过滤掉标记为删除的帖子。
     */
    @Select("SELECT * FROM `post_index` WHERE `user_id`=#{user} AND `is_deleted` = 0 AND create_time < #{timestamp} ORDER BY create_time DESC LIMIT 0, #{size}")
    List<PostIndex> getAllValidPostIndexFromOutbox(int user, Timestamp timestamp, int size);

    /**
     * 获取所指定用户的 inbox 中所有的帖子索引，但是过滤掉标记为删除的帖子。
     */
    @Select("SELECT * FROM `inbox` WHERE `user_id`=#{user} AND `is_deleted` = 0 AND create_time < #{timestamp} ORDER BY create_time DESC LIMIT 0, #{size}")
    List<PostIndex> getAllValidPostIndexFromInbox(int user, Timestamp timestamp, int size);

}
