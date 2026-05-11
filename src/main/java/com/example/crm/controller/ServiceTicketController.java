
package com.example.crm.controller;

import java.util.List;
import java.util.Map;
import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.ServiceTicket;
import com.example.crm.service.ServiceTicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/services")
public class ServiceTicketController {

    private final ServiceTicketService serviceTicketService;

    public ServiceTicketController(ServiceTicketService serviceTicketService) {
        this.serviceTicketService = serviceTicketService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ServiceTicket>>> listTickets(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        
        PageResponse<ServiceTicket> response = serviceTicketService.listTickets(pageNum, pageSize, keyword, status, priority);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceTicket>> getTicket(@PathVariable Long id) {
        ServiceTicket ticket = serviceTicketService.getTicketById(id);
        if (ticket == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "服务工单不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceTicket>> createTicket(@RequestBody ServiceTicket ticket) {
        ServiceTicket created = serviceTicketService.createTicket(ticket);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceTicket>> updateTicket(@PathVariable Long id, @RequestBody ServiceTicket ticket) {
        ServiceTicket updated = serviceTicketService.updateTicket(id, ticket);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable Long id) {
        serviceTicketService.deleteTicket(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> batchDeleteTickets(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(400, "请选择要删除的服务单"));
        }
        serviceTicketService.batchDeleteTickets(ids);
        return ResponseEntity.ok(ApiResponse.success("批量删除成功", null));
    }
}
