package cn.atsukoruo.AuthorizationService.Repository;



import cn.atsukoruo.common.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface UserMapper {
    @Select("SELECT * FROM user WHERE username=#{username}")
    User selectByUsername(String username);

    @Select("SELECT * FROM user WHERE id=#{userId}")
    User selectById(int userId);

    @Select("SELECT COUNT(*) FROM user WHERE username=#{username}")
    int doesExistUser(String username);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO user(username, password, avatar_url, is_banned, is_influencer, create_time, role, nickname)" +
            "VALUES(#{username}, #{password}, #{avatar_url}, #{isBanned}, #{isInfluencer}, #{createTime}, #{role}, #{nickname})" )
    void insertUser(User user);
}
