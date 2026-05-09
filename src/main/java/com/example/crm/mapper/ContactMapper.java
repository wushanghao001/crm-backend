
package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.Contact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContactMapper extends BaseMapper<Contact> {

    @Select("SELECT * FROM contact WHERE customer_id = #{customerId}")
    List<Contact> findByCustomerId(@Param("customerId") Long customerId);
}
