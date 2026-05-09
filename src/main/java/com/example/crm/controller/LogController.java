package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.OperationLog;
import com.example.crm.service.OperationLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final OperationLogService logService;

    public LogController(OperationLogService logService) {
        this.logService = logService;
    }

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<PageResponse<OperationLog>>> listLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        PageResponse<OperationLog> response = logService.listLogs(pageNum, pageSize, operator, type, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<OperationLog>> getLog(@PathVariable Long id) {
        OperationLog log = logService.getLogById(id);
        if (log == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "日志不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(log));
    }
}
