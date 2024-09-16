package cn.atsukoruo.societyservice.Repository;

import cn.atsukoruo.societyservice.Entity.Post;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.jdbc.SQL;
import java.sql.Timestamp;;
import java.util.List;

@NoArgsConstructor
@Slf4j
public class PostProvider {
    public String copyAllPostsToInbox(Integer from, Integer to, List<Post> posts) {
        SQL sql = new SQL();
        sql.INSERT_INTO("inbox(user_id, post_id, create_time, from_id)");
        StringBuilder builder = new StringBuilder();
        builder.append(" VALUES ");
        for (Post post : posts) {
            String value = "(" + to + "," +  post.getId() + "," + post.getCreateTime().getTime() + "," +  from + "),";
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

    public String selectAllPostByIds(List<Integer> ids, boolean keepDeleted) {
        SQL sql = new SQL();
        sql.SELECT("*").FROM("post");
        StringBuilder builder = new StringBuilder();
        builder.append(" WHERE id IN (");
        for (Integer id : ids) {
           String value = id + ",";
           builder.append(value);
        }
        String postfix = keepDeleted ? ")" : "AND is_deleted=0)" + "ORDER BY create_time DESC";
        return sql + builder.substring(0, builder.length() - 1) + postfix;
    }

    public String retrievePost(int user, Timestamp timestamp, int size, boolean keepDeleted) {
        return keepDeleted ?
            "SELECT * FROM post WHERE user_id=" + user + " AND create_time < " + timestamp.getTime() + " ORDER BY create_time DESC LIMIT 0," + size:
            "SELECT * FROM post WHERE user_id=" + user + " AND create_time < " + timestamp.getTime() + " AND is_deleted = 0 ORDER BY create_time DESC LIMIT 0," + size;
    }
}
