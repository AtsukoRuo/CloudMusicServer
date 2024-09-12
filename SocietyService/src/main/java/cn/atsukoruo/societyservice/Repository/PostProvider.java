package cn.atsukoruo.societyservice.Repository;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.jdbc.SQL;
import org.bouncycastle.util.Times;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

@NoArgsConstructor
@Slf4j
public class PostProvider {
    public String insertToInbox(Integer from,  Integer postId, List<Integer> ids) {
        SQL sql = new SQL();
        sql.INSERT_INTO("inbox(user_id, post_id, create_time, from_id)");
        StringBuilder builder = new StringBuilder();
        builder.append(" VALUES ");
        for (Integer id : ids) {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String value = "(" + id + "," +  postId + ",'" + timestamp +"'," +  from + "),";
            builder.append((value));
        }
        return sql + builder.substring(0, builder.length() - 1);
    }
}
