package cn.atsukoruo.societyservice.Repository;


import cn.atsukoruo.common.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface UserMapper {
    @Select("SELECT is_influencer FROM user WHERE id = #{userId}")
    Boolean isInfluencer(int userId);

    @SelectProvider(type = UserProvider.class, method = "selectAllUser")
    List<User> selectAllUser(List<Integer> users);
}
