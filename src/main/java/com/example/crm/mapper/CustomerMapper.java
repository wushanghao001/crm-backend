
package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    @Select("SELECT * FROM customer WHERE name LIKE #{keyword} OR phone LIKE #{keyword} OR email LIKE #{keyword}")
    List<Customer> search(@Param("keyword") String keyword);

    @Select("SELECT * FROM customer WHERE status = #{status}")
    List<Customer> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM customer WHERE industry = #{industry}")
    List<Customer> findByIndustry(@Param("industry") String industry);
}
