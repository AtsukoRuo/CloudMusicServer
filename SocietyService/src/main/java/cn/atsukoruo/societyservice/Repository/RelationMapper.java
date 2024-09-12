package cn.atsukoruo.societyservice.Repository;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Mapper
@Repository
public interface RelationMapper {

    @Insert("INSERT INTO relationship(`from`, `to`, `relation`) VALUES (#{from}, #{to}, #{relation})")
    void createRelation(int from ,int to, int relation);

    @Delete("DELETE FROM relationship WHERE `from`=#{from} AND `to`=#{to}")
    void removeRelation(int from, int to);

    @Update("UPDATE `relationship` SET `relation`=#{newRelation} WHERE `from`=#{from} AND `to`=#{to}")
    void updateRelation(int from, int to, int newRelation);


    @Select("SELECT `relation` FROM `relationship` WHERE `from` = #{from} AND `to` = #{to}")
    Integer getRelation(int from ,int to);


    @Select("SELECT COUNT(*) FROM `relationship` WHERE `from`=#{user} AND (relation = 1 OR relation = 4)")
    Integer getFollowedUserNum(Integer user);


    @Select("SELECT COUNT(*) FROM `relationship` WHERE `from`=#{user} AND (relation = 0 OR relation = 4)")
    Integer getFollowingUserNum(Integer user);


    @Select("SELECT `to` FROM `relationship` WHERE `from`=#{user} AND (`relation`=1 OR `relation`=4) ORDER BY `to` LIMIT #{from}, #{size}")
    List<Integer> getFollowedUser(int user, int from, int size);

    @Select("SELECT `to` FROM `relationship` WHERE `from`=#{user} AND (`relation` = 0 OR `relation` = 4) ORDER BY `to` LIMIT #{from}, #{size}")
    List<Integer> getFollowingUser(int user, int from, int size);
}
