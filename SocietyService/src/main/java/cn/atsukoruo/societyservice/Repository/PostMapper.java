package cn.atsukoruo.societyservice.Repository;

import cn.atsukoruo.societyservice.Entity.Post;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

@Mapper
@Repository
public interface PostMapper {
    @Select("SELECT id FROM post where user_id=#{userId}")
    List<Integer> getPostIdsOfUser(int userId);

    @InsertProvider(type = PostProvider.class, method = "insertToInbox")
    void insertToInbox(Integer from, Integer inbox, List<Integer> ids);

    @Delete("DELETE FROM inbox WHERE from_id=#{removedUserId} AND user_id=#{userId}")
    void deletePostInInbox(Integer userId, Integer removedUserId);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT post(id, create_time, content, img_url, user_id, is_deleted)" +
            "VALUES(#{id}, #{createTime}, #{content}, #{imgUrl}, #{userId}, #{isDeleted})")
    void insertPost(Post post);

    @Select("SELECT * FROM post WHERE user_id=#{userId} AND create_time < timestamp ORDER BY timestamp LIMIT 0, #{size}")
    List<Post> selectPosts(int userId, Date timestamp, int size);
}
