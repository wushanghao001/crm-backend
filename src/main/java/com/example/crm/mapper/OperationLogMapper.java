package com.example.crm.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface OperationLogMapper extends com.baomidou.mybatisplus.core.mapper.BaseMapper<OperationLog> {
    
    default IPage<OperationLog> selectPageWithFilters(Page<OperationLog> page, String operator, String type, 
                                                      LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLog> queryWrapper = new LambdaQueryWrapper<>();
        
        if (operator != null && !operator.isEmpty()) {
            queryWrapper.like(OperationLog::getOperator, operator);
        }
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq(OperationLog::getType, type);
        }
        if (startTime != null) {
            queryWrapper.ge(OperationLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(OperationLog::getCreatedAt, endTime);
        }
        
        queryWrapper.orderByDesc(OperationLog::getCreatedAt);
        
        return selectPage(page, queryWrapper);
    }
}
