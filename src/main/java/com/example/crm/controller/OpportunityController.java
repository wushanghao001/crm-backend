
package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.Opportunity;
import com.example.crm.service.OpportunityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> listOpportunities(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stage) {

        PageResponse<Map<String, Object>> response = opportunityService.listOpportunities(pageNum, pageSize, keyword, stage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Opportunity>> getOpportunity(@PathVariable Long id) {
        Opportunity opportunity = opportunityService.getOpportunityById(id);
        if (opportunity == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "销售机会不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(opportunity));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Opportunity>> createOpportunity(@RequestBody Opportunity opportunity) {
        Opportunity created = opportunityService.createOpportunity(opportunity);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Opportunity>> updateOpportunity(@PathVariable Long id, @RequestBody Opportunity opportunity) {
        Opportunity updated = opportunityService.updateOpportunity(id, opportunity);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOpportunity(@PathVariable Long id) {
        opportunityService.deleteOpportunity(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
