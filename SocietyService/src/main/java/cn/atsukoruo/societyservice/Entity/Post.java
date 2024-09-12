package cn.atsukoruo.societyservice.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Post {
    Integer id;
    Timestamp createTime;
    String content;
    String imgUrl;
    Integer userId;
    boolean isDeleted;
}
