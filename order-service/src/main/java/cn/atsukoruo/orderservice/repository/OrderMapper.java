package cn.atsukoruo.orderservice.repository;

import cn.atsukoruo.orderservice.configuration.EnumCodeTypeHandler;
import cn.atsukoruo.orderservice.entity.Order;
import cn.atsukoruo.orderservice.entity.OrderStatus;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface OrderMapper {

    @Insert("INSERT INTO `order`(`order_number`, `user`, `product`, `create_time`, `price`, `status`, `amount`)" +
            " VALUES(#{orderNumber}, #{user}, #{product}, #{createTime}, #{price}, #{status}, #{amount})")
    void createOrder(Order order);

    @Select("SELECT * FROM `order` WHERE `user`=#{user}")
    List<Order> queryOrdersByUser(Integer user);

    @Update("UPDATE `order` SET status = #{status} WHERE order_number = #{tradeOrderId}")
    void payOrder(String tradeOrderId, OrderStatus status);
}
