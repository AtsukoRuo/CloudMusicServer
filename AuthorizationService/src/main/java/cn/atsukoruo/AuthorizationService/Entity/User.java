package cn.atsukoruo.AuthorizationService.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class User {
    int id;
    String username;
    String nickname;
    String password;
    String salt;
    String role;
    String avatar_url;
    String phone;
    String introduction;
    boolean isBanned;
    boolean isInfluencer;
    Date createTime;
}
