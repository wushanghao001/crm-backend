package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.entity.Customer;
import com.example.crm.entity.CustomerFollow;
import com.example.crm.entity.Task;
import com.example.crm.entity.User;
import com.example.crm.mapper.CustomerFollowMapper;
import com.example.crm.mapper.CustomerMapper;
import com.example.crm.mapper.TaskMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final CustomerFollowMapper customerFollowMapper;
    private final CustomerMapper customerMapper;

    public TaskService(TaskMapper taskMapper, CustomerFollowMapper customerFollowMapper, CustomerMapper customerMapper) {
        this.taskMapper = taskMapper;
        this.customerFollowMapper = customerFollowMapper;
        this.customerMapper = customerMapper;
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new IllegalStateException("无法获取当前用户信息");
    }

    public Map<String, Object> listTasks(Integer pageNum, Integer pageSize, String status) {
        User currentUser = getCurrentUser();
        
        List<Task> allTasks = new ArrayList<>();

        // 1. 获取手工创建的任务
        LambdaQueryWrapper<Task> taskQuery = new LambdaQueryWrapper<>();
        if (!"admin".equals(currentUser.getRole())) {
            taskQuery.eq(Task::getAssigneeId, currentUser.getId().intValue());
        }
        if (status != null && !status.isEmpty()) {
            taskQuery.eq(Task::getStatus, status);
        }
        List<Task> manualTasks = taskMapper.selectList(taskQuery);
        allTasks.addAll(manualTasks);

        // 2. 从跟进记录中提取每个客户最新的下次跟进提醒
        List<Task> followTasks = extractTasksFromFollowRecords(currentUser.getId().intValue(), status);
        allTasks.addAll(followTasks);

        // 3. 按下次跟进时间排序（优先显示最近需要跟进的），时间相同则按创建时间排序
        allTasks.sort((t1, t2) -> {
            LocalDateTime dueDate1 = t1.getDueDate() != null ? t1.getDueDate() : LocalDateTime.MAX;
            LocalDateTime dueDate2 = t2.getDueDate() != null ? t2.getDueDate() : LocalDateTime.MAX;
            int dateCompare = dueDate1.compareTo(dueDate2);
            if (dateCompare != 0) {
                return dateCompare;
            }
            LocalDateTime createdAt1 = t1.getCreatedAt() != null ? t1.getCreatedAt() : LocalDateTime.MIN;
            LocalDateTime createdAt2 = t2.getCreatedAt() != null ? t2.getCreatedAt() : LocalDateTime.MIN;
            return createdAt2.compareTo(createdAt1);
        });

        // 4. 分页处理
        int total = allTasks.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<Task> pageList = start < total ? allTasks.subList(start, end) : new ArrayList<>();

        Map<String, Object> response = new HashMap<>();
        response.put("list", pageList);
        response.put("total", total);
        response.put("pageNum", pageNum);
        response.put("pageSize", pageSize);

        return response;
    }

    private List<Task> extractTasksFromFollowRecords(Integer userId, String status) {
        List<Task> tasks = new ArrayList<>();

        // 查询当前用户有下次跟进时间的跟进记录
        LambdaQueryWrapper<CustomerFollow> followQuery = Wrappers.lambdaQuery(CustomerFollow.class)
                .eq(CustomerFollow::getFollowUserId, userId)
                .isNotNull(CustomerFollow::getNextFollowTime)
                .orderByDesc(CustomerFollow::getCreatedAt);
        
        List<CustomerFollow> follows = customerFollowMapper.selectList(followQuery);

        // 按客户分组，只保留每个客户最新的一条下次跟进记录
        Map<Integer, CustomerFollow> latestFollowByCustomer = new LinkedHashMap<>();
        for (CustomerFollow follow : follows) {
            int customerId = follow.getCustomerId();
            if (!latestFollowByCustomer.containsKey(customerId)) {
                latestFollowByCustomer.put(customerId, follow);
            }
        }

        // 获取客户信息 - 使用Long类型的Map
        List<Integer> customerIds = new ArrayList<>(latestFollowByCustomer.keySet());
        Map<Long, Customer> customerMap = new HashMap<>();
        if (!customerIds.isEmpty()) {
            List<Customer> customers = customerMapper.selectBatchIds(customerIds);
            for (Customer customer : customers) {
                customerMap.put(customer.getId(), customer);
            }
        }

        // 转换为任务格式
        for (Map.Entry<Integer, CustomerFollow> entry : latestFollowByCustomer.entrySet()) {
            CustomerFollow follow = entry.getValue();
            Customer customer = customerMap.get(entry.getKey().longValue()); // 转换为Long类型

            // 如果指定了状态筛选，跳过不匹配的
            if ("completed".equals(status)) {
                continue; // 跟进记录提取的任务默认都是待处理状态
            }

            // 检查该客户在这条跟进记录之后是否有已支付的跟进记录
            if (hasPaymentRecordAfterFollow(entry.getKey(), follow.getCreatedAt())) {
                continue; // 如果有已支付记录，跳过这条提醒
            }

            Task task = new Task();
            task.setId(-follow.getId()); // 使用负数表示这是从跟进记录生成的虚拟任务
            task.setTitle("跟进客户: " + (customer != null ? customer.getName() : "未知客户"));
            task.setContent(follow.getRemark());
            task.setTaskType("follow");
            task.setRelatedCustomerId(follow.getCustomerId());
            task.setRelatedFollowId(follow.getId());
            task.setDueDate(follow.getNextFollowTime());
            task.setPriority("medium");
            task.setStatus("pending");
            task.setCreatedAt(follow.getCreatedAt());
            task.setUpdatedAt(follow.getUpdatedAt());

            tasks.add(task);
        }

        return tasks;
    }

    /**
     * 检查客户在指定时间之后是否有已支付的跟进记录
     * @param customerId 客户ID
     * @param afterTime 时间点
     * @return 如果有已支付记录返回true，否则返回false
     */
    private boolean hasPaymentRecordAfterFollow(Integer customerId, LocalDateTime afterTime) {
        LambdaQueryWrapper<CustomerFollow> query = Wrappers.lambdaQuery(CustomerFollow.class)
                .eq(CustomerFollow::getCustomerId, customerId)
                .gt(CustomerFollow::getCreatedAt, afterTime)
                .in(CustomerFollow::getFollowResult, "paid", "closed");
        
        Long count = customerFollowMapper.selectCount(query);
        return count != null && count > 0;
    }

    public Task createTask(Task task) {
        User currentUser = getCurrentUser();
        task.setCreatorId(currentUser.getId().intValue());
        task.setAssigneeId(currentUser.getId().intValue());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        if (task.getStatus() == null) {
            task.setStatus("pending");
        }
        // 手工创建的任务设置为personal类型
        task.setTaskType("personal");
        taskMapper.insert(task);
        return task;
    }

    public Task updateTask(Integer id, Task task) {
        Task existing = taskMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !currentUser.getId().equals(Long.valueOf(existing.getAssigneeId()))) {
            throw new IllegalArgumentException("无权修改此任务");
        }

        task.setId(id);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        return taskMapper.selectById(id);
    }

    public void deleteTask(Integer id) {
        Task existing = taskMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !currentUser.getId().equals(Long.valueOf(existing.getCreatorId()))) {
            throw new IllegalArgumentException("无权删除此任务");
        }

        taskMapper.deleteById(id);
    }

    public Task toggleTaskStatus(Integer id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        User currentUser = getCurrentUser();
        if (!"admin".equals(currentUser.getRole()) && !currentUser.getId().equals(Long.valueOf(task.getAssigneeId()))) {
            throw new IllegalArgumentException("无权修改此任务");
        }

        if ("pending".equals(task.getStatus())) {
            task.setStatus("completed");
        } else {
            task.setStatus("pending");
        }
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        return task;
    }
}