package cn.atsukoruo.societyservice.repository;

import cn.atsukoruo.societyservice.entity.PostIndex;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.jdbc.SQL;
;
import java.util.List;

@NoArgsConstructor
@Slf4j
public class PostProvider {
    public String insertAllPostIndexToInbox(Integer inbox, List<PostIndex> posts) {
        SQL sql = new SQL();
        sql.INSERT_INTO("inbox(user_id, post_id, create_time, from_id)");
        StringBuilder builder = new StringBuilder();
        builder.append(" VALUES ");
        for (PostIndex post : posts) {
            String value = "(" + inbox  + "," +  post.getId() + "," + post.getCreateTime() + "," +  post.getUserId() + "),";
            builder.append((value));
        }
        return sql + builder.substring(0, builder.length() - 1);
    }

    public String publishPostToFollowedUser(Integer from,  Integer postId, long timestamp, List<Integer> ids) {
        SQL sql = new SQL();
        sql.INSERT_INTO("inbox(user_id, post_id, create_time, from_id)");
        StringBuilder builder = new StringBuilder();
        builder.append(" VALUES ");
        for (Integer id : ids) {
            String value = "(" + id + "," +  postId + "," + timestamp + "," +  from + "),";
            builder.append((value));
        }
        return sql + builder.substring(0, builder.length() - 1);
    }

    public String retrievePostByIds(List<Integer> ids) {
        SQL sql = new SQL();
        sql.SELECT("*").FROM("post");
        StringBuilder builder = new StringBuilder();
        builder.append(" WHERE id IN (");
        for (Integer id : ids) {
           String value = id + ",";
           builder.append(value);
        }
        String postfix =") ORDER BY create_time DESC";
        return sql + builder.substring(0, builder.length() - 1) + postfix;
    }
}
