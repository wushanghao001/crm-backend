
package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crm.entity.ServiceTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceTicketMapper extends BaseMapper<ServiceTicket> {

    @Select("SELECT * FROM service_ticket WHERE status = #{status}")
    List<ServiceTicket> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM service_ticket WHERE assignee_id = #{assigneeId}")
    List<ServiceTicket> findByAssigneeId(@Param("assigneeId") Long assigneeId);
}
