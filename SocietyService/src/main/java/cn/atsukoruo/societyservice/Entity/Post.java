package cn.atsukoruo.societyservice.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Post {
    Integer id;
    Date creatTime;
    String content;
    String imgUrl;
    Integer userId;
    boolean isDeleted;
}
