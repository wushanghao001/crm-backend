
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
import com.example.crm.mapper.UserMapper;
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
    private final UserMapper userMapper;

    public OpportunityService(OpportunityMapper opportunityMapper, CustomerMapper customerMapper, UserMapper userMapper) {
        this.opportunityMapper = opportunityMapper;
        this.customerMapper = customerMapper;
        this.userMapper = userMapper;
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

        List<Long> ownerIds = result.getRecords().stream()
                .map(Opportunity::getOwnerId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        System.out.println("DEBUG ownerIds: " + ownerIds);

        Map<Long, String> ownerNameMapFinal = new java.util.HashMap<>();
        if (!ownerIds.isEmpty()) {
            List<User> owners = userMapper.selectBatchIds(ownerIds);
            System.out.println("DEBUG owners: " + owners);
            if (owners != null && !owners.isEmpty()) {
                ownerNameMapFinal = owners.stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
            }
        }
        System.out.println("DEBUG ownerNameMapFinal: " + ownerNameMapFinal);

        final Map<Long, String> customerNameMap = customerNameMapFinal;
        final Map<Long, String> ownerNameMap = ownerNameMapFinal;

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
            map.put("ownerName", ownerNameMap.getOrDefault(opp.getOwnerId(), "未知"));
            map.put("createdAt", opp.getCreatedAt());
            map.put("updatedAt", opp.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());

        PageResponse<Map<String, Object>> response = new PageResponse<>(records, result.getTotal(), pageNum, pageSize);
        return response;
    }

    public Map<String, Object> getOpportunityById(Long id) {
        Opportunity opportunity = opportunityMapper.selectById(id);
        if (opportunity == null) {
            throw new IllegalArgumentException("销售机会不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !opportunity.getOwnerId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("无权访问此销售机会");
        }

        String ownerName = "未知";
        if (opportunity.getOwnerId() != null) {
            User owner = userMapper.selectById(opportunity.getOwnerId());
            if (owner != null) {
                ownerName = owner.getUsername();
            }
        }

        String customerName = "未知客户";
        if (opportunity.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(opportunity.getCustomerId());
            if (customer != null) {
                customerName = customer.getName();
            }
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", opportunity.getId());
        result.put("customerId", opportunity.getCustomerId());
        result.put("customerName", customerName);
        result.put("name", opportunity.getName());
        result.put("stage", opportunity.getStage());
        result.put("amount", opportunity.getAmount());
        result.put("probability", opportunity.getProbability());
        result.put("expectedCloseDate", opportunity.getExpectedCloseDate());
        result.put("description", opportunity.getDescription());
        result.put("ownerId", opportunity.getOwnerId());
        result.put("ownerName", ownerName);
        result.put("createdAt", opportunity.getCreatedAt());
        result.put("updatedAt", opportunity.getUpdatedAt());
        return result;
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

    public void batchDeleteOpportunities(List<Long> ids) {
        User currentUser = getCurrentUser();
        boolean isAdmin = "admin".equals(currentUser.getRole());

        for (Long id : ids) {
            Opportunity existing = opportunityMapper.selectById(id);
            if (existing != null) {
                if (!isAdmin && !existing.getOwnerId().equals(currentUser.getId())) {
                    throw new IllegalArgumentException("无权删除销售机会: " + existing.getName());
                }
            }
        }

        opportunityMapper.deleteBatchIds(ids);
    }
}
