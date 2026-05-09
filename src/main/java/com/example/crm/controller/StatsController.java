package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.entity.User;
import com.example.crm.mapper.CustomerMapper;
import com.example.crm.mapper.OrderMapper;
import com.example.crm.mapper.ServiceTicketMapper;
import com.example.crm.security.JwtAuthenticationToken;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;
    private final ServiceTicketMapper serviceTicketMapper;

    public StatsController(CustomerMapper customerMapper, OrderMapper orderMapper, ServiceTicketMapper serviceTicketMapper) {
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.serviceTicketMapper = serviceTicketMapper;
    }

    @GetMapping("/personal")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPersonalStats() {
        User currentUser = getCurrentUser();
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