package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.Product;
import com.example.crm.entity.StockLog;
import com.example.crm.service.StockLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
public class StockLogController {

    private final StockLogService stockLogService;

    public StockLogController(StockLogService stockLogService) {
        this.stockLogService = stockLogService;
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<PageResponse<StockLog>>> getStockLogs(
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResponse<StockLog> result = stockLogService.getStockLogs(productId, pageNum, pageSize);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/logs/order/{orderId}")
    public ResponseEntity<ApiResponse<List<StockLog>>> getStockLogsByOrderId(@PathVariable Long orderId) {
        List<StockLog> result = stockLogService.getStockLogsByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/warning")
    public ResponseEntity<ApiResponse<List<Product>>> getLowStockProducts() {
        List<Product> result = stockLogService.getLowStockProducts();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> checkStockAvailable(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        boolean available = stockLogService.checkStockAvailable(productId, quantity);
        return ResponseEntity.ok(ApiResponse.success(available));
    }
}
