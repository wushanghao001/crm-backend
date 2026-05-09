package com.example.crm.controller;

import com.example.crm.dto.FunnelResponse;
import com.example.crm.dto.StageCustomersResponse;
import com.example.crm.dto.TrendResponse;
import com.example.crm.service.FunnelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/funnel")
public class FunnelController {

    private final FunnelService funnelService;

    public FunnelController(FunnelService funnelService) {
        this.funnelService = funnelService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getFunnelData(
            @RequestParam(defaultValue = "today") String timeRange) {
        FunnelResponse funnel = funnelService.getFunnelData(timeRange);
        return ResponseEntity.ok(Map.of("code", 200, "data", funnel));
    }

    @GetMapping("/trend")
    public ResponseEntity<Map<String, Object>> getTrendData(
            @RequestParam(defaultValue = "today") String timeRange) {
        TrendResponse trend = funnelService.getTrendData(timeRange);
        return ResponseEntity.ok(Map.of("code", 200, "data", trend));
    }

    @GetMapping("/customers")
    public ResponseEntity<Map<String, Object>> getStageCustomers(
            @RequestParam String stage) {
        StageCustomersResponse response = funnelService.getStageCustomers(stage);
        return ResponseEntity.ok(Map.of("code", 200, "data", response));
    }
}