package com.example.crm.controller;

import com.example.crm.config.CacheConfig;
import com.example.crm.dto.ApiResponse;
import com.example.crm.entity.User;
import com.example.crm.mapper.CustomerMapper;
import com.example.crm.mapper.OrderMapper;
import com.example.crm.mapper.ServiceTicketMapper;
import com.example.crm.security.JwtAuthenticationToken;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private static final Logger log = LoggerFactory.getLogger(StatsController.class);

    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;
    private final ServiceTicketMapper serviceTicketMapper;
    private final Cache<String, Object> personalStatsCache;

    public StatsController(CustomerMapper customerMapper, OrderMapper orderMapper,
                          ServiceTicketMapper serviceTicketMapper,
                          @Qualifier("personalStatsCache") Cache<String, Object> personalStatsCache) {
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.serviceTicketMapper = serviceTicketMapper;
        this.personalStatsCache = personalStatsCache;
    }

    @GetMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshCache() {
        personalStatsCache.invalidate(CacheConfig.PERSONAL_STATS_CACHE);
        return ResponseEntity.ok(ApiResponse.success("刷新成功"));
    }

    @GetMapping("/personal")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPersonalStats() {
        System.out.println("[StatsController] /personal endpoint called");
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            System.out.println("[StatsController] User is null, returning 401");
            return ResponseEntity.ok(ApiResponse.error(401, "未登录"));
        }

        String cacheKey = CacheConfig.PERSONAL_STATS_CACHE + "_" + currentUser.getId();
        System.out.println("[StatsController] Cache key: " + cacheKey + ", userId: " + currentUser.getId());

        @SuppressWarnings("unchecked")
        Map<String, Object> cached = (Map<String, Object>) personalStatsCache.getIfPresent(cacheKey);
        if (cached != null) {
            System.out.println("[StatsController] Cache HIT for key: " + cacheKey);
            return ResponseEntity.ok(ApiResponse.success(cached));
        }

        System.out.println("[StatsController] Cache MISS for key: " + cacheKey + ", loading from database");

        Map<String, Object> stats = new HashMap<>();

        Long myCustomers = customerMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.crm.entity.Customer>()
                .eq(com.example.crm.entity.Customer::getCreatorId, currentUser.getId())
        );

        Long publicCustomers = customerMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.crm.entity.Customer>()
                .isNull(com.example.crm.entity.Customer::getCreatorId)
        );

        Long myOrders = orderMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.crm.entity.Order>()
                .eq(com.example.crm.entity.Order::getCreatorId, currentUser.getId())
        );

        Long pendingServices = serviceTicketMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.crm.entity.ServiceTicket>()
                .eq(com.example.crm.entity.ServiceTicket::getAssigneeId, currentUser.getId())
                .eq(com.example.crm.entity.ServiceTicket::getStatus, "pending")
        );

        stats.put("myCustomers", myCustomers);
        stats.put("publicCustomers", publicCustomers);
        stats.put("myOrders", myOrders);
        stats.put("pendingServices", pendingServices);

        personalStatsCache.put(cacheKey, stats);
        System.out.println("[StatsController] Cache PUT for key: " + cacheKey);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken) {
            return (User) auth.getPrincipal();
        }
        return null;
    }
}