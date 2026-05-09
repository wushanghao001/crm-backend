
package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.Customer;
import com.example.crm.entity.Opportunity;
import com.example.crm.entity.User;
import com.example.crm.mapper.CustomerMapper;
import com.example.crm.mapper.OpportunityMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OpportunityService {

    private final OpportunityMapper opportunityMapper;
    private final CustomerMapper customerMapper;

    public OpportunityService(OpportunityMapper opportunityMapper, CustomerMapper customerMapper) {
        this.opportunityMapper = opportunityMapper;
        this.customerMapper = customerMapper;
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new IllegalStateException("无法获取当前用户信息");
    }

    public PageResponse<Map<String, Object>> listOpportunities(Integer pageNum, Integer pageSize, String keyword, String stage) {
        Page<Opportunity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Opportunity> queryWrapper = new LambdaQueryWrapper<>();

        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        if (!isAdmin) {
            queryWrapper.eq(Opportunity::getOwnerId, currentUser.getId());
        }

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(Opportunity::getName, keyword);
        }
        if (stage != null && !stage.isEmpty()) {
            queryWrapper.eq(Opportunity::getStage, stage);
        }

        queryWrapper.orderByDesc(Opportunity::getCreatedAt);
        IPage<Opportunity> result = opportunityMapper.selectPage(page, queryWrapper);

        List<Long> customerIds = result.getRecords().stream()
                .map(Opportunity::getCustomerId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> customerNameMapFinal = new java.util.HashMap<>();
        if (!customerIds.isEmpty()) {
            List<Customer> customers = customerMapper.selectBatchIds(customerIds);
            customerNameMapFinal = customers.stream()
                    .collect(Collectors.toMap(Customer::getId, Customer::getName));
        }

        final Map<Long, String> customerNameMap = customerNameMapFinal;

        List<Map<String, Object>> records = result.getRecords().stream().map(opp -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", opp.getId());
            map.put("customerId", opp.getCustomerId());
            map.put("customerName", customerNameMap.getOrDefault(opp.getCustomerId(), "未知客户"));
            map.put("name", opp.getName());
            map.put("stage", opp.getStage());
            map.put("amount", opp.getAmount());
            map.put("probability", opp.getProbability());
            map.put("expectedCloseDate", opp.getExpectedCloseDate());
            map.put("description", opp.getDescription());
            map.put("ownerId", opp.getOwnerId());
            map.put("createdAt", opp.getCreatedAt());
            map.put("updatedAt", opp.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());

        PageResponse<Map<String, Object>> response = new PageResponse<>(records, result.getTotal(), pageNum, pageSize);
        return response;
    }

    public Opportunity getOpportunityById(Long id) {
        Opportunity opportunity = opportunityMapper.selectById(id);
        if (opportunity == null) {
            throw new IllegalArgumentException("销售机会不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !opportunity.getOwnerId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("无权访问此销售机会");
        }

        return opportunity;
    }

    public Opportunity createOpportunity(Opportunity opportunity) {
        User currentUser = getCurrentUser();
        opportunity.setOwnerId(currentUser.getId());
        opportunity.setCreatedAt(LocalDateTime.now());
        opportunity.setUpdatedAt(LocalDateTime.now());

        opportunityMapper.insert(opportunity);
        return opportunity;
    }

    public Opportunity updateOpportunity(Long id, Opportunity opportunity) {
        Opportunity existing = opportunityMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("销售机会不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !existing.getOwnerId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("无权修改此销售机会");
        }

        existing.setName(opportunity.getName());
        existing.setStage(opportunity.getStage());
        existing.setAmount(opportunity.getAmount());
        existing.setProbability(opportunity.getProbability());
        existing.setExpectedCloseDate(opportunity.getExpectedCloseDate());
        existing.setDescription(opportunity.getDescription());
        existing.setUpdatedAt(LocalDateTime.now());

        opportunityMapper.updateById(existing);
        return existing;
    }

    public void deleteOpportunity(Long id) {
        Opportunity existing = opportunityMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("销售机会不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !existing.getOwnerId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("无权删除此销售机会");
        }

        opportunityMapper.deleteById(id);
    }
}
