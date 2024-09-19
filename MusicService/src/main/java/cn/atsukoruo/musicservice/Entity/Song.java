package cn.atsukoruo.musicservice.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Song {
    Integer id;
    String title;
    String singer;
    String songUrl;
    String imgUrl;
    String lrcUrl;
}
