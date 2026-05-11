package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.BrandCode;
import com.example.crm.service.BrandCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/brand-codes")
public class BrandCodeController {

    private final BrandCodeService brandCodeService;

    public BrandCodeController(BrandCodeService brandCodeService) {
        this.brandCodeService = brandCodeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BrandCode>>> listBrandCodes(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        PageResponse<BrandCode> response = brandCodeService.listBrandCodes(pageNum, pageSize, keyword);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandCode>> getBrandCode(@PathVariable Long id) {
        BrandCode brandCode = brandCodeService.getBrandCodeById(id);
        if (brandCode == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "牌号不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(brandCode));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BrandCode>> createBrandCode(@RequestBody BrandCode brandCode) {
        BrandCode created = brandCodeService.createBrandCode(brandCode);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandCode>> updateBrandCode(@PathVariable Long id, @RequestBody BrandCode brandCode) {
        BrandCode updated = brandCodeService.updateBrandCode(id, brandCode);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBrandCode(@PathVariable Long id) {
        brandCodeService.deleteBrandCode(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> batchDeleteBrandCodes(@RequestBody Map<String, Long[]> request) {
        Long[] ids = request.get("ids");
        brandCodeService.batchDeleteBrandCodes(ids);
        return ResponseEntity.ok(ApiResponse.success("批量删除成功", null));
    }

    @GetMapping("/check-code")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkCodeExists(
            @RequestParam String code,
            @RequestParam(required = false) Long excludeId) {
        boolean exists = brandCodeService.checkCodeExists(code, excludeId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("exists", exists)));
    }
}