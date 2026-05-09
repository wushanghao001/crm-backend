package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.CustomerFollowDTO;
import com.example.crm.entity.Customer;
import com.example.crm.entity.CustomerFollow;
import com.example.crm.entity.Task;
import com.example.crm.entity.User;
import com.example.crm.mapper.CustomerFollowMapper;
import com.example.crm.mapper.CustomerMapper;
import com.example.crm.mapper.TaskMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomerFollowService {

    private final CustomerFollowMapper customerFollowMapper;
    private final CustomerMapper customerMapper;
    private final TaskMapper taskMapper;

    public CustomerFollowService(CustomerFollowMapper customerFollowMapper, CustomerMapper customerMapper, TaskMapper taskMapper) {
        this.customerFollowMapper = customerFollowMapper;
        this.customerMapper = customerMapper;
        this.taskMapper = taskMapper;
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new IllegalStateException("无法获取当前用户信息");
    }

    public Map<String, Object> listFollows(Integer customerId, Integer pageNum, Integer pageSize, String keyword, String followType, String followResult, String intentLevel, Integer followUserId) {
        User currentUser = getCurrentUser();
        Page<CustomerFollow> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CustomerFollow> queryWrapper = new LambdaQueryWrapper<>();

        if (customerId != null) {
            queryWrapper.eq(CustomerFollow::getCustomerId, customerId);
        }

        if (!"admin".equals(currentUser.getRole())) {
            queryWrapper.eq(CustomerFollow::getFollowUserId, currentUser.getId().intValue());
        } else if (followUserId != null) {
            queryWrapper.eq(CustomerFollow::getFollowUserId, followUserId);
        }

        if (keyword != null && !keyword.isEmpty()) {
            LambdaQueryWrapper<Customer> customerQuery = new LambdaQueryWrapper<>();
            customerQuery.like(Customer::getName, keyword);
            List<Customer> matchedCustomers = customerMapper.selectList(customerQuery);
            if (!matchedCustomers.isEmpty()) {
                List<Integer> customerIds = matchedCustomers.stream()
                        .map(c -> c.getId().intValue())
                        .collect(Collectors.toList());
                queryWrapper.in(CustomerFollow::getCustomerId, customerIds);
            } else {
                queryWrapper.like(CustomerFollow::getContent, keyword);
            }
        }
        if (followType != null && !followType.isEmpty()) {
            queryWrapper.eq(CustomerFollow::getFollowType, followType);
        }
        if (followResult != null && !followResult.isEmpty()) {
            queryWrapper.eq(CustomerFollow::getFollowResult, followResult);
        }
        if (intentLevel != null && !intentLevel.isEmpty()) {
            queryWrapper.eq(CustomerFollow::getIntentLevel, intentLevel);
        }

        queryWrapper.orderByDesc(CustomerFollow::getCreatedAt);
        IPage<CustomerFollow> result = customerFollowMapper.selectPage(page, queryWrapper);

        List<CustomerFollow> follows = result.getRecords();
        List<Integer> customerIds = follows.stream()
                .map(CustomerFollow::getCustomerId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, String> customerNameMap = new HashMap<>();
        if (!customerIds.isEmpty()) {
            for (Integer cid : customerIds) {
                Customer customer = customerMapper.selectById(cid);
                if (customer != null) {
                    customerNameMap.put(cid, customer.getName());
                }
            }
        }

        List<Integer> userIds = follows.stream()
                .map(CustomerFollow::getFollowUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, String> userNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Integer userId : userIds) {
                userNameMap.put(userId, "用户" + userId);
            }
        }

        List<CustomerFollowDTO> dtoList = follows.stream().map(f -> {
            CustomerFollowDTO dto = new CustomerFollowDTO();
            dto.setId(f.getId());
            dto.setCustomerId(f.getCustomerId());
            dto.setCustomerName(customerNameMap.getOrDefault(f.getCustomerId(), "未知客户"));
            dto.setFollowType(f.getFollowType());
            dto.setFollowResult(f.getFollowResult());
            dto.setIntentLevel(f.getIntentLevel());
            dto.setContent(f.getContent());
            dto.setRemark(f.getRemark());
            dto.setNextFollowTime(f.getNextFollowTime());
            dto.setAttachment(f.getAttachment());
            dto.setRemindFlag(f.getRemindFlag());
            dto.setFollowUserId(f.getFollowUserId());
            dto.setFollowUserName(userNameMap.getOrDefault(f.getFollowUserId(), "未知"));
            dto.setCreatedAt(f.getCreatedAt());
            dto.setUpdatedAt(f.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("list", dtoList);
        response.put("total", result.getTotal());
        response.put("pageNum", pageNum);
        response.put("pageSize", pageSize);

        return response;
    }

    public CustomerFollowDTO getFollowById(Integer id) {
        CustomerFollow follow = customerFollowMapper.selectById(id);
        if (follow == null) {
            throw new IllegalArgumentException("跟进记录不存在");
        }

        CustomerFollowDTO dto = new CustomerFollowDTO();
        dto.setId(follow.getId());
        dto.setCustomerId(follow.getCustomerId());
        dto.setFollowType(follow.getFollowType());
        dto.setFollowResult(follow.getFollowResult());
        dto.setIntentLevel(follow.getIntentLevel());
        dto.setContent(follow.getContent());
        dto.setRemark(follow.getRemark());
        dto.setNextFollowTime(follow.getNextFollowTime());
        dto.setAttachment(follow.getAttachment());
        dto.setRemindFlag(follow.getRemindFlag());
        dto.setFollowUserId(follow.getFollowUserId());
        dto.setCreatedAt(follow.getCreatedAt());
        dto.setUpdatedAt(follow.getUpdatedAt());

        return dto;
    }

    @Transactional
    public CustomerFollowDTO createFollow(CustomerFollow follow) {
        User currentUser = getCurrentUser();
        follow.setFollowUserId(currentUser.getId().intValue());
        follow.setCreatedAt(LocalDateTime.now());
        follow.setUpdatedAt(LocalDateTime.now());

        customerFollowMapper.insert(follow);

        updateCustomerFollowInfo(follow.getCustomerId());

        if (follow.getNextFollowTime() != null) {
            Task task = new Task();
            task.setTitle("请及时跟进客户");
            task.setContent("请及时跟进客户ID: " + follow.getCustomerId());
            task.setTaskType("follow");
            task.setRelatedCustomerId(follow.getCustomerId());
            task.setRelatedFollowId(follow.getId());
            task.setDueDate(follow.getNextFollowTime());
            task.setPriority("medium");
            task.setStatus("pending");
            task.setAssigneeId(currentUser.getId().intValue());
            task.setCreatorId(currentUser.getId().intValue());
            task.setCreatedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.insert(task);
        }

        CustomerFollowDTO dto = new CustomerFollowDTO();
        dto.setId(follow.getId());
        dto.setCustomerId(follow.getCustomerId());
        dto.setFollowType(follow.getFollowType());
        dto.setFollowResult(follow.getFollowResult());
        dto.setIntentLevel(follow.getIntentLevel());
        dto.setContent(follow.getContent());
        dto.setRemark(follow.getRemark());
        dto.setNextFollowTime(follow.getNextFollowTime());
        dto.setAttachment(follow.getAttachment());
        dto.setRemindFlag(follow.getRemindFlag());
        dto.setFollowUserId(follow.getFollowUserId());
        dto.setFollowUserName("用户" + follow.getFollowUserId());
        dto.setCreatedAt(follow.getCreatedAt());
        dto.setUpdatedAt(follow.getUpdatedAt());

        return dto;
    }

    @Transactional
    public CustomerFollowDTO updateFollow(Integer id, CustomerFollow follow) {
        CustomerFollow existing = customerFollowMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("跟进记录不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !existing.getFollowUserId().equals(currentUser.getId().intValue())) {
            throw new IllegalArgumentException("无权修改此跟进记录");
        }

        follow.setId(id);
        follow.setFollowUserId(existing.getFollowUserId());
        follow.setUpdatedAt(LocalDateTime.now());

        customerFollowMapper.updateById(follow);

        CustomerFollowDTO dto = new CustomerFollowDTO();
        dto.setId(follow.getId());
        dto.setCustomerId(follow.getCustomerId());
        dto.setFollowType(follow.getFollowType());
        dto.setFollowResult(follow.getFollowResult());
        dto.setIntentLevel(follow.getIntentLevel());
        dto.setContent(follow.getContent());
        dto.setRemark(follow.getRemark());
        dto.setNextFollowTime(follow.getNextFollowTime());
        dto.setAttachment(follow.getAttachment());
        dto.setRemindFlag(follow.getRemindFlag());
        dto.setFollowUserId(follow.getFollowUserId());
        dto.setCreatedAt(existing.getCreatedAt());
        dto.setUpdatedAt(follow.getUpdatedAt());

        return dto;
    }

    @Transactional
    public void deleteFollow(Integer id) {
        CustomerFollow existing = customerFollowMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("跟进记录不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !existing.getFollowUserId().equals(currentUser.getId().intValue())) {
            throw new IllegalArgumentException("无权删除此跟进记录");
        }

        customerFollowMapper.deleteById(id);
    }

    private void updateCustomerFollowInfo(Integer customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer != null) {
            customer.setLastFollowTime(LocalDateTime.now());

            LambdaQueryWrapper<CustomerFollow> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CustomerFollow::getCustomerId, customerId);
            long followCount = customerFollowMapper.selectCount(queryWrapper);
            customer.setFollowCount((int) followCount);

            LambdaQueryWrapper<CustomerFollow> latestWrapper = new LambdaQueryWrapper<>();
            latestWrapper.eq(CustomerFollow::getCustomerId, customerId);
            latestWrapper.orderByDesc(CustomerFollow::getCreatedAt);
            latestWrapper.last("LIMIT 1");
            CustomerFollow latestFollow = customerFollowMapper.selectOne(latestWrapper);

            if (latestFollow != null) {
                String result = latestFollow.getFollowResult();
                String status = customer.getStatus();
                if (status == null) {
                    status = "active";
                }

                if ("closed".equals(result)) {
                    customer.setStatus("closed");
                } else if ("lost".equals(result) || "contact_lost".equals(result)) {
                    customer.setStatus("lost");
                } else if ("initial_contact".equals(result) || "requirement".equals(result) ||
                           "quotation".equals(result) || "negotiation".equals(result) ||
                           "pending_deal".equals(result)) {
                    customer.setStatus("following");
                }
            }

            customerMapper.updateById(customer);
        }
    }
}