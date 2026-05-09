package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.example.crm.entity.Task;
import com.example.crm.entity.User;
import com.example.crm.mapper.TaskMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class TaskService {

    private final TaskMapper taskMapper;

    public TaskService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
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
        Page<Task> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();

        if (!"admin".equals(currentUser.getRole())) {
            queryWrapper.eq(Task::getAssigneeId, currentUser.getId().intValue());
        }

        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Task::getStatus, status);
        }

        queryWrapper.orderByDesc(Task::getCreatedAt);
        Page<Task> result = taskMapper.selectPage(page, queryWrapper);

        Map<String, Object> response = new HashMap<>();
        response.put("list", result.getRecords());
        response.put("total", result.getTotal());
        response.put("pageNum", pageNum);
        response.put("pageSize", pageSize);

        return response;
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