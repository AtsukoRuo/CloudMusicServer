package cn.atsukoruo.productservice.repository;

import cn.atsukoruo.productservice.entity.Product;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface ProductMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO product(name, price, introduction) " +
            "VALUES(#{name}, #{price}, #{introduction})")
    void insertProduct(Product product);


    @Select("SELECT * FROM product WHERE id=#{id}")
    Product queryProductById(Integer id);
}
