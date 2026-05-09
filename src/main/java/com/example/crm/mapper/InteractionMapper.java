
package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.Interaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InteractionMapper extends BaseMapper<Interaction> {

    @Select("SELECT * FROM interaction WHERE customer_id = #{customerId} ORDER BY interaction_time DESC")
    List<Interaction> findByCustomerId(@Param("customerId") Long customerId);
}
