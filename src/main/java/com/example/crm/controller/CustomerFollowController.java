package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.CustomerFollowDTO;
import com.example.crm.entity.CustomerFollow;
import com.example.crm.service.CustomerFollowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/follow")
public class CustomerFollowController {

    private final CustomerFollowService customerFollowService;

    public CustomerFollowController(CustomerFollowService customerFollowService) {
        this.customerFollowService = customerFollowService;
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listFollows(
            @RequestParam(required = false) Integer customerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String followType,
            @RequestParam(required = false) String followResult,
            @RequestParam(required = false) String intentLevel,
            @RequestParam(required = false) Integer followUserId) {

        Map<String, Object> result = customerFollowService.listFollows(
                customerId, pageNum, pageSize, keyword, followType, followResult, intentLevel, followUserId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<ApiResponse<CustomerFollowDTO>> getFollowDetail(@PathVariable Integer id) {
        CustomerFollowDTO dto = customerFollowService.getFollowById(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CustomerFollowDTO>> createFollow(@RequestBody CustomerFollow follow) {
        CustomerFollowDTO dto = customerFollowService.createFollow(follow);
        return ResponseEntity.ok(ApiResponse.success("创建成功", dto));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<CustomerFollowDTO>> updateFollow(@PathVariable Integer id, @RequestBody CustomerFollow follow) {
        CustomerFollowDTO dto = customerFollowService.updateFollow(id, follow);
        return ResponseEntity.ok(ApiResponse.success("更新成功", dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFollow(@PathVariable Integer id) {
        customerFollowService.deleteFollow(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @GetMapping("/pending-tasks")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingFollowTasks(
            @RequestParam(required = false) Integer customerId) {
        List<Map<String, Object>> tasks = customerFollowService.getPendingFollowTasks(customerId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }
}