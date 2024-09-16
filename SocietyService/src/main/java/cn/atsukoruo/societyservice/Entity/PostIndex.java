package cn.atsukoruo.societyservice.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostIndex {
    Integer id;
    Timestamp createTime;
    Integer userId;
    Boolean isDeleted;
}
