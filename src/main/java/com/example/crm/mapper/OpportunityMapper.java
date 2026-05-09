
package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.Opportunity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OpportunityMapper extends BaseMapper<Opportunity> {

    @Select("SELECT * FROM opportunity WHERE stage = #{stage}")
    List<Opportunity> findByStage(@Param("stage") String stage);
}
