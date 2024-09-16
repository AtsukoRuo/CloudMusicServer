package cn.atsukoruo.AuthorizationService.Repository;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
@DS("mysql")
public interface TokenMapper {
    @Select("SELECT version FROM token_version where user_id = #{userId} and client = #{client}")
    public Integer selectVersion(int userId, String client);

    @Select("SELECT batch FROM token_batch where user_id = #{user_id}")
    public Integer selectBatch(int userId);

    @Update("UPDATE token_version SET version = version + #{diff} WHERE user_id = #{userId} and client = #{client}")
    public int addVersion(int userId, String client, int diff);

    @Update("UPDATE token_batch SET batch = batch + #{diff} where user_id = #{userId}")
    public int addBatch(int userId, int diff);

    @Insert("INSERT INTO token_version(user_id, client, version) VALUES(#{userId}, #{client}, #{version})")
    public void insertVersion(int userId, String client, int version);

    @Insert("INSERT INTO token_batch(user_id, batch) VALUES(#{userId}, #{batch})")
    public void insertBatch(int userId, int batch);
}
