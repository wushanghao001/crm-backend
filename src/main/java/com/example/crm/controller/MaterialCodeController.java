package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.MaterialCode;
import com.example.crm.service.MaterialCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/material-codes")
public class MaterialCodeController {

    private final MaterialCodeService materialCodeService;

    public MaterialCodeController(MaterialCodeService materialCodeService) {
        this.materialCodeService = materialCodeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MaterialCode>>> listMaterialCodes(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        PageResponse<MaterialCode> response = materialCodeService.listMaterialCodes(pageNum, pageSize, keyword);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialCode>> getMaterialCode(@PathVariable Long id) {
        MaterialCode materialCode = materialCodeService.getMaterialCodeById(id);
        if (materialCode == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "料号不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(materialCode));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MaterialCode>> createMaterialCode(@RequestBody MaterialCode materialCode) {
        MaterialCode created = materialCodeService.createMaterialCode(materialCode);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialCode>> updateMaterialCode(@PathVariable Long id, @RequestBody MaterialCode materialCode) {
        MaterialCode updated = materialCodeService.updateMaterialCode(id, materialCode);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMaterialCode(@PathVariable Long id) {
        materialCodeService.deleteMaterialCode(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> batchDeleteMaterialCodes(@RequestBody Map<String, Long[]> request) {
        Long[] ids = request.get("ids");
        materialCodeService.batchDeleteMaterialCodes(ids);
        return ResponseEntity.ok(ApiResponse.success("批量删除成功", null));
    }

    @GetMapping("/check-code")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkCodeExists(
            @RequestParam String code,
            @RequestParam(required = false) Long excludeId) {
        boolean exists = materialCodeService.checkCodeExists(code, excludeId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("exists", exists)));
    }
}