package cn.atsukoruo.AuthorizationService.Repository;

import org.apache.ibatis.jdbc.SQL;

import java.util.List;

public class UserProvider {
    public String selectAllUsers(List<Integer> users) {
        String base = "SELECT id, nickname, avatar_url FROM user WHERE `id` IN (";
        StringBuilder builder = new StringBuilder();
        builder.append(base);
        for (Integer user : users) {
            builder.append(user).append(",");
        }
        return builder.substring(0, builder.length() - 1) + ")";
    }
}
