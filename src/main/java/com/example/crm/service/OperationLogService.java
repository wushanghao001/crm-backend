package com.example.crm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.OperationLog;
import com.example.crm.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperationLogService {

    private final OperationLogMapper logMapper;

    public OperationLogService(OperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    public PageResponse<OperationLog> listLogs(Integer pageNum, Integer pageSize, String operator, 
                                                String type, LocalDateTime startTime, LocalDateTime endTime) {
        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        IPage<OperationLog> result = logMapper.selectPageWithFilters(page, operator, type, startTime, endTime);
        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public OperationLog getLogById(Long id) {
        return logMapper.selectById(id);
    }

    public void saveLog(OperationLog log) {
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
    }

    public void saveLog(String operator, String operatorId, String type, String content, 
                        String ip, String params, String result, Integer status, String module) {
        OperationLog log = new OperationLog();
        log.setOperator(operator);
        log.setOperatorId(operatorId);
        log.setType(type);
        log.setContent(content);
        log.setIp(ip);
        log.setParams(params);
        log.setResult(result);
        log.setStatus(status);
        log.setModule(module);
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
    }
}
