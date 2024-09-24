package cn.atsukoruo.authorization.repository;


import cn.atsukoruo.authorization.entity.User;
import cn.atsukoruo.common.entity.UserInfo;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
@DS("sharding")
public interface UserMapper {
    @Select("SELECT is_influencer FROM user WHERE id=#{userId}")
    Boolean isInfluencer(int userId);

    @Select("SELECT * FROM user WHERE username=#{username}")
    User selectByUsername(String username);

    @Select("SELECT * FROM user WHERE id=#{userId}")
    @Results({
        @Result(property = "isBanned", column = "is_banned", javaType = boolean.class),
        @Result(property = "isInfluencer", column = "is_influencer", javaType = boolean.class)
    })
    User selectById(int userId);

    @SelectProvider(type = UserProvider.class, method = "selectAllUsers")
    List<UserInfo> selectAllUsers(List<Integer> users);

    @Select("SELECT COUNT(*) FROM user WHERE username=#{username}")
    int doesExistUser(String username);

    @Insert("INSERT INTO user(id, username, password, avatar_url, is_banned, is_influencer, create_time, role, nickname)" +
            "VALUES(#{id}, #{username}, #{password}, #{avatar_url}, #{isBanned}, #{isInfluencer}, #{createTime}, #{role}, #{nickname})" )
    void insertUser(User user);

    @Select("SELECT * FROM user WHERE vx_openId=#{vxOpenId}")
    User getUserByVxOpenId(String vxOpenId);

    @Update("UPDATE user SET vx_openId='' WHERE vx_openId=#{vxOpenId}")
    void deleteOpenIdField(String vxOpenId);

    @Update("UPDATE user SET vx_openId=#{vxOpenId} WHERE id=#{user}")
    void insertVxOpenIdToUser(Integer user, String vxOpenId);

    @Select("SELECT vx_openId FROM user WHERE id=#{user}")
    String getVxOpenIdFromUser(Integer user);
}
