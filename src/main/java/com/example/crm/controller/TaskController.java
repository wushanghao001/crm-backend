package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.entity.Task;
import com.example.crm.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listTasks(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        Map<String, Object> result = taskService.listTasks(pageNum, pageSize, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Task>> createTask(@RequestBody Task task) {
        Task result = taskService.createTask(task);
        return ResponseEntity.ok(ApiResponse.success("创建成功", result));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<Task>> updateTask(@PathVariable Integer id, @RequestBody Task task) {
        Task result = taskService.updateTask(id, task);
        return ResponseEntity.ok(ApiResponse.success("更新成功", result));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Integer id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PutMapping("/toggle/{id}")
    public ResponseEntity<ApiResponse<Task>> toggleTaskStatus(@PathVariable Integer id) {
        Task result = taskService.toggleTaskStatus(id);
        return ResponseEntity.ok(ApiResponse.success("状态更新成功", result));
    }
}