package cn.atsukoruo.AuthorizationService.Entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class User {
    int id;
    String username;
    String nickname;
    String password;
    String role;
    String avatar_url;
    boolean isBanned;
    boolean isInfluencer;
    Timestamp createTime;
}