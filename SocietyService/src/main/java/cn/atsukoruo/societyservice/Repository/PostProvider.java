package cn.atsukoruo.societyservice.Repository;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.jdbc.SQL;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

@NoArgsConstructor
@Slf4j
public class PostProvider {
    public String insertToInbox(Integer from,  Integer inbox, List<Integer> ids) {
        SQL sql = new SQL();
        sql.INSERT_INTO("inbox(user_id, post_id, create_time, from_id)");
        StringBuilder builder = new StringBuilder();
        builder.append(" VALUES ");
        for (Integer id : ids) {
            Date date = new Date(System.currentTimeMillis());
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String value = "(" + inbox + "," + id + ",'" + df.format(date) +"'," +  from + "),";
            builder.append((value));
        }
        String ret = sql + builder.substring(0, builder.length() - 1);
        log.debug(ret);
        return ret;
    }
}
