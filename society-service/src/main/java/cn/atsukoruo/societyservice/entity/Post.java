package cn.atsukoruo.societyservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Post {
    String content;
    String imgUrl;
    Integer postId;
    Integer userId;
}
