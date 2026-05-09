
package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("SELECT * FROM product WHERE category = #{category}")
    List<Product> findByCategory(@Param("category") String category);

    @Select("SELECT * FROM product WHERE status = #{status}")
    List<Product> findByStatus(@Param("status") Integer status);
}
