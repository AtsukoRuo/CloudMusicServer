package cn.atsukoruo.societyservice.Repository;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

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
}
