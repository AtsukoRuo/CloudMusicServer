package cn.atsukoruo.societyservice.Repository;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface RelationMapper {

    @Insert("INSERT INTO relationship(`from`, `to`, `relation`) VALUES (#{from}, #{to}, #{relation})")
    void createRelation(int from ,int to, int relation);

    @Delete("DELETE FROM relationship WHERE `from`=#{from} AND `to`=#{to}")
    void removeRelation(int from, int to);

    @Update("UPDATE `relationship` SET `relation`=#{newRelation} WHERE `from`=#{from} AND `to`=#{to}")
    void updateRelation(int from, int to, int newRelation);


    // TODO 这个上个读锁
    @Select("SELECT `relation` FROM `relationship` WHERE `from` = #{from} AND `to` = #{to}")
    Integer getRelation(int from ,int to);


}
