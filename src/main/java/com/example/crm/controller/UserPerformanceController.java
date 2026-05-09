package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.PageResponse;
import com.example.crm.dto.UserPerformanceResponse;
import com.example.crm.dto.UserPerformanceTrendResponse;
import com.example.crm.entity.Order;
import com.example.crm.service.UserPerformanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/user/performance")
public class UserPerformanceController {

    private final UserPerformanceService userPerformanceService;

    public UserPerformanceController(UserPerformanceService userPerformanceService) {
        this.userPerformanceService = userPerformanceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserPerformanceResponse>> getPerformance(
            @RequestParam(defaultValue = "month") String timeRange) {
        UserPerformanceResponse response = userPerformanceService.getPerformance(timeRange);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<UserPerformanceTrendResponse>> getTrend(
            @RequestParam(defaultValue = "6") Integer months) {
        UserPerformanceTrendResponse response = userPerformanceService.getTrend(months);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<PageResponse<Order>>> getOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        PageResponse<Order> response = userPerformanceService.getOrders(pageNum, pageSize, keyword, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/target")
    public ResponseEntity<ApiResponse<Void>> updateTarget(@RequestBody Map<String, BigDecimal> request) {
        BigDecimal target = request.get("monthTarget");
        userPerformanceService.updateTarget(target);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}