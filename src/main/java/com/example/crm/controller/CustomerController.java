package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.CustomerRequest;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.Customer;
import com.example.crm.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Customer>>> listCustomers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false, defaultValue = "my") String listType) {

        PageResponse<Customer> response = customerService.listCustomers(pageNum, pageSize, keyword, status, industry, listType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<Customer>>> listPublicCustomers() {
        List<Customer> customers = customerService.getPublicCustomers();
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getCustomerStats() {
        Long myCustomers = customerService.countMyCustomers();
        Long publicCustomers = customerService.countPublicCustomers();
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of(
            "myCustomers", myCustomers,
            "publicCustomers", publicCustomers
        )));
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<ApiResponse<Customer>> claimCustomer(@PathVariable Long id) {
        Customer customer = customerService.claimCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("认领成功", customer));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<Customer>> assignCustomer(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        Customer customer = customerService.assignCustomer(id, userId);
        return ResponseEntity.ok(ApiResponse.success("分配成功", customer));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomer(@PathVariable Long id) {
        Map<String, Object> customer = customerService.getCustomerById(id);
        if (customer == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "客户不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Customer>> createCustomer(@Valid @RequestBody CustomerRequest request) {
        Customer customer = customerService.createCustomer(request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Customer>> updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        Customer customer = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", customer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> batchDeleteCustomers(@RequestBody java.util.Map<String, Long[]> request) {
        Long[] ids = request.get("ids");
        customerService.batchDeleteCustomers(ids);
        return ResponseEntity.ok(ApiResponse.success("批量删除成功", null));
    }
}