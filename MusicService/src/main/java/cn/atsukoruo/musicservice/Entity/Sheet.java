package cn.atsukoruo.musicservice.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Sheet {
    Integer id;
    Integer userId;;
    String imgUrl;
    Timestamp createTime;
    String title;
}