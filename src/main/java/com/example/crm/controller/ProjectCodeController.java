package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.ProjectCode;
import com.example.crm.service.ProjectCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/project-codes")
public class ProjectCodeController {

    private final ProjectCodeService projectCodeService;

    public ProjectCodeController(ProjectCodeService projectCodeService) {
        this.projectCodeService = projectCodeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProjectCode>>> listProjectCodes(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        PageResponse<ProjectCode> response = projectCodeService.listProjectCodes(pageNum, pageSize, keyword);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectCode>> getProjectCode(@PathVariable Long id) {
        ProjectCode projectCode = projectCodeService.getProjectCodeById(id);
        if (projectCode == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "项目号不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(projectCode));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectCode>> createProjectCode(@RequestBody ProjectCode projectCode) {
        ProjectCode created = projectCodeService.createProjectCode(projectCode);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectCode>> updateProjectCode(@PathVariable Long id, @RequestBody ProjectCode projectCode) {
        ProjectCode updated = projectCodeService.updateProjectCode(id, projectCode);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProjectCode(@PathVariable Long id) {
        projectCodeService.deleteProjectCode(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> batchDeleteProjectCodes(@RequestBody Map<String, Long[]> request) {
        Long[] ids = request.get("ids");
        projectCodeService.batchDeleteProjectCodes(ids);
        return ResponseEntity.ok(ApiResponse.success("批量删除成功", null));
    }

    @GetMapping("/check-code")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkCodeExists(
            @RequestParam String code,
            @RequestParam(required = false) Long excludeId) {
        boolean exists = projectCodeService.checkCodeExists(code, excludeId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("exists", exists)));
    }
}