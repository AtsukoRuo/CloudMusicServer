package cn.atsukoruo.orderservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Order {
    Integer id;
    Long orderNumber;
    Integer user;
    Integer product;
    Timestamp createTime;
    BigDecimal price;
    OrderStatus status;
    Integer amount;
    String title;
}
