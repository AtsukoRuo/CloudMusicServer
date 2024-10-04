package cn.atsukoruo.invendtoryservice.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface InventoryMapper {

    @Insert("INSERT INTO inventory(id, amount) VALUES(#{productId}, #{amount})")
    void createInventory(Integer productId, Integer amount);

    @Update("UPDATE inventory SET amount = amount - #{diff} WHERE id=#{productId}")
    void updateInventory(Integer productId, Integer diff);

    @Select("SELECT amount FROM inventory WHERE id=#{productId}")
    Integer queryInventory(Integer productId);
}
