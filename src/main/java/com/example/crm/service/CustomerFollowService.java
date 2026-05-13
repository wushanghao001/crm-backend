package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.dto.CustomerFollowDTO;
import com.example.crm.entity.Contact;
import com.example.crm.entity.Customer;
import com.example.crm.entity.CustomerFollow;
import com.example.crm.entity.Order;
import com.example.crm.entity.Opportunity;
import com.example.crm.entity.Task;
import com.example.crm.entity.User;
import com.example.crm.mapper.ContactMapper;
import com.example.crm.mapper.CustomerFollowMapper;
import com.example.crm.mapper.CustomerMapper;
import com.example.crm.mapper.OrderMapper;
import com.example.crm.mapper.OpportunityMapper;
import com.example.crm.mapper.TaskMapper;
import com.example.crm.mapper.UserMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomerFollowService {

    private final CustomerFollowMapper customerFollowMapper;
    private final CustomerMapper customerMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final ContactMapper contactMapper;
    private final OrderMapper orderMapper;
    private final OpportunityMapper opportunityMapper;

    public CustomerFollowService(CustomerFollowMapper customerFollowMapper, CustomerMapper customerMapper, 
            TaskMapper taskMapper, UserMapper userMapper, ContactMapper contactMapper, 
            OrderMapper orderMapper, OpportunityMapper opportunityMapper) {
        this.customerFollowMapper = customerFollowMapper;
        this.customerMapper = customerMapper;
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
        this.contactMapper = contactMapper;
        this.orderMapper = orderMapper;
        this.opportunityMapper = opportunityMapper;
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new IllegalStateException("无法获取当前用户信息");
    }

    public Map<String, Object> listFollows(Integer customerId, Integer pageNum, Integer pageSize, String keyword, String followType, String followResult, String intentLevel, Integer followUserId, String startDate, String endDate) {
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
        if (startDate != null && !startDate.isEmpty()) {
            LocalDate start = LocalDate.parse(startDate);
            queryWrapper.ge(CustomerFollow::getCreatedAt, start.atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDate end = LocalDate.parse(endDate);
            queryWrapper.le(CustomerFollow::getCreatedAt, end.atTime(LocalTime.MAX));
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
                User user = userMapper.selectById(userId);
                userNameMap.put(userId, user != null ? user.getUsername() : "未知用户");
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

            dto.setContactName(getCustomerContactName(f.getCustomerId()));
            dto.setTotalPaidAmount(getCustomerTotalPaidAmount(f.getCustomerId()));
            dto.setCurrentStage(getCustomerCurrentStage(f.getCustomerId()));

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

        if (follow.getFollowUserId() != null) {
            User user = userMapper.selectById(follow.getFollowUserId());
            dto.setFollowUserName(user != null ? user.getUsername() : "未知用户");
        } else {
            dto.setFollowUserName("未知");
        }

        dto.setContactName(getCustomerContactName(follow.getCustomerId()));
        dto.setTotalPaidAmount(getCustomerTotalPaidAmount(follow.getCustomerId()));
        dto.setCurrentStage(getCustomerCurrentStage(follow.getCustomerId()));

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

        if (follow.getNextFollowTime() != null && Boolean.TRUE.equals(follow.getRemindFlag())) {
            Task task = new Task();
            task.setTitle("请及时跟进客户");
            Customer customer = customerMapper.selectById(follow.getCustomerId());
            String customerName = customer != null ? customer.getName() : "未知客户";
            StringBuilder content = new StringBuilder(customerName);
            if (follow.getRemark() != null && !follow.getRemark().isEmpty()) {
                content.append(" - ").append(follow.getRemark());
            }
            task.setContent(content.toString());
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

        dto.setContactName(getCustomerContactName(follow.getCustomerId()));
        dto.setTotalPaidAmount(getCustomerTotalPaidAmount(follow.getCustomerId()));
        dto.setCurrentStage(getCustomerCurrentStage(follow.getCustomerId()));

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

        dto.setContactName(getCustomerContactName(follow.getCustomerId()));
        dto.setTotalPaidAmount(getCustomerTotalPaidAmount(follow.getCustomerId()));
        dto.setCurrentStage(getCustomerCurrentStage(follow.getCustomerId()));

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

        LambdaQueryWrapper<Task> taskQueryWrapper = new LambdaQueryWrapper<>();
        taskQueryWrapper.eq(Task::getRelatedFollowId, id);
        taskMapper.delete(taskQueryWrapper);

        Integer customerId = existing.getCustomerId();
        customerFollowMapper.deleteById(id);

        updateCustomerFollowCount(customerId);
    }

    private void updateCustomerFollowCount(Integer customerId) {
        if (customerId == null) return;
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) return;

        LambdaQueryWrapper<CustomerFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CustomerFollow::getCustomerId, customerId);
        long followCount = customerFollowMapper.selectCount(queryWrapper);
        customer.setFollowCount((int) followCount);
        customerMapper.updateById(customer);
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
                    status = "potential";
                }

                if ("lost".equals(result) || "contact_lost".equals(result)) {
                    customer.setStatus("churned");
                } else {
                    customer.setStatus("active");
                }
            }

            customerMapper.updateById(customer);
        }
    }

    private String getCustomerContactName(Integer customerId) {
        if (customerId == null) return null;
        LambdaQueryWrapper<Contact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Contact::getCustomerId, customerId.longValue());
        queryWrapper.orderByDesc(Contact::getCreatedAt);
        queryWrapper.last("LIMIT 1");
        Contact contact = contactMapper.selectOne(queryWrapper);
        return contact != null ? contact.getName() : null;
    }

    private BigDecimal getCustomerTotalPaidAmount(Integer customerId) {
        if (customerId == null) return BigDecimal.ZERO;
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getCustomerId, customerId.longValue());
        queryWrapper.eq(Order::getStatus, "completed");
        queryWrapper.eq(Order::getPayStatus, "paid");
        List<Order> orders = orderMapper.selectList(queryWrapper);
        BigDecimal total = BigDecimal.ZERO;
        for (Order order : orders) {
            if (order.getPaidAmount() != null) {
                total = total.add(order.getPaidAmount());
            }
        }
        return total;
    }

    private String getCustomerCurrentStage(Integer customerId) {
        if (customerId == null) return null;
        LambdaQueryWrapper<Opportunity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Opportunity::getCustomerId, customerId.longValue());
        queryWrapper.orderByDesc(Opportunity::getCreatedAt);
        queryWrapper.last("LIMIT 1");
        Opportunity opportunity = opportunityMapper.selectOne(queryWrapper);
        return opportunity != null ? opportunity.getStage() : null;
    }

    public List<Map<String, Object>> getPendingFollowTasks(Integer customerId) {
        User currentUser = getCurrentUser();
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Task::getTaskType, "follow");
        queryWrapper.eq(Task::getStatus, "pending");
        queryWrapper.eq(Task::getAssigneeId, currentUser.getId().intValue());
        if (customerId != null) {
            queryWrapper.eq(Task::getRelatedCustomerId, customerId);
        }
        queryWrapper.orderByDesc(Task::getDueDate);

        List<Task> tasks = taskMapper.selectList(queryWrapper);

        List<Integer> followIds = tasks.stream()
                .map(Task::getRelatedFollowId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, CustomerFollow> followMap = new HashMap<>();
        if (!followIds.isEmpty()) {
            List<CustomerFollow> follows = customerFollowMapper.selectBatchIds(followIds);
            for (CustomerFollow follow : follows) {
                followMap.put(follow.getId(), follow);
            }
        }

        List<Integer> customerIds = tasks.stream()
                .map(Task::getRelatedCustomerId)
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

        return tasks.stream().map(task -> {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("id", task.getId());
            taskInfo.put("title", task.getTitle());
            taskInfo.put("dueDate", task.getDueDate());
            taskInfo.put("priority", task.getPriority());
            taskInfo.put("status", task.getStatus());

            Integer followId = task.getRelatedFollowId();
            if (followId != null && followMap.containsKey(followId)) {
                CustomerFollow follow = followMap.get(followId);
                taskInfo.put("customerId", follow.getCustomerId());
                taskInfo.put("customerName", customerNameMap.getOrDefault(follow.getCustomerId(), "未知客户"));
                taskInfo.put("followContent", follow.getContent());
                taskInfo.put("remark", follow.getRemark());
            }

            Integer taskCustomerId = task.getRelatedCustomerId();
            if (taskCustomerId != null) {
                taskInfo.put("customerName", customerNameMap.getOrDefault(taskCustomerId, "未知客户"));
            }

            return taskInfo;
        }).collect(Collectors.toList());
    }
}