package cn.atsukoruo.societyservice.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Post {
    Integer id;
    Integer userId;
    String content;
    String imgUrl;
    Timestamp createTime;
}
