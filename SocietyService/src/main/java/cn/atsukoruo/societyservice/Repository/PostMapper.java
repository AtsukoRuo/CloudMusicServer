package cn.atsukoruo.societyservice.Repository;

import cn.atsukoruo.societyservice.Entity.Post;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

@Mapper
@Repository
public interface PostMapper {

    /**
     获取指定用户的所有帖子，但是过滤掉标记为删除的帖子
     */
    @Select("SELECT * FROM `post` WHERE `user_id`=#{userId} AND `is_deleted` = 0")
    List<Post> getPostOfUser(int userId);

    /**
     * 获取所指定用户的所有帖子的摘要信息，包括 id、create_time，但是过滤掉标记为删除的帖子。
     */
    @Select("SELECT `id`, `create_time` FROM `post` WHERE `user_id`=#{userId} AND `is_deleted` = 0")
    List<Post> getPostInfoOfUser(int userId);

    /**
     *
     * @param from      发帖人
     * @param postId    帖子 ID
     * @param ids       向这些用户推送该帖子
     * @param timestamp 帖子的时间戳
     */
    @InsertProvider(type = PostProvider.class, method = "insertToInbox")
    void insertToInbox(Integer from, Integer postId,  long timestamp, List<Integer> ids);

    @Delete("DELETE FROM inbox WHERE from_id=#{removedUserId} AND user_id=#{userId}")
    void deletePostFromUserInInbox(Integer userId, Integer removedUserId);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT post(id, create_time, content, img_url, user_id, is_deleted)" +
            "VALUES(#{id}, #{createTime}, #{content}, #{imgUrl}, #{userId}, #{isDeleted})")
    Integer insertPost(Post post);

    @Select("SELECT * FROM post WHERE user_id=#{userId} AND create_time < timestamp ORDER BY timestamp LIMIT 0, #{size}")
    List<Post> selectPosts(int userId, Date timestamp, int size);

    @Update("UPDATE `post` SET `is_deleted` = 1 WHERE `id` = #{post} AND `user_id`=#{user}")
    void deletePostSoftlyInOutbox(int user, int post);
}
