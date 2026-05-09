
package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.MenuResponse;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.Role;
import com.example.crm.service.MenuService;
import com.example.crm.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;
    private final MenuService menuService;

    public RoleController(RoleService roleService, MenuService menuService) {
        this.roleService = roleService;
        this.menuService = menuService;
    }

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<PageResponse<Role>>> listRoles(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name) {
        
        PageResponse<Role> response = roleService.listRoles(pageNum, pageSize, name);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<Role>> getRole(@PathVariable Long id) {
        Role role = roleService.getRoleById(id);
        if (role == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "角色不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getPermissionTree() {
        List<MenuResponse> tree = menuService.getPermissionTree();
        return ResponseEntity.ok(ApiResponse.success(tree));
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<Role>> createRole(@RequestBody Role role) {
        Role created = roleService.createRole(role);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<Role>> updateRole(@PathVariable Long id, @RequestBody Role role) {
        Role updated = roleService.updateRole(id, role);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<Void>> savePermissions(@PathVariable Long id, @RequestBody Map<String, List<String>> body) {
        List<String> permissions = body.get("permissions");
        roleService.saveRolePermissions(id, permissions);
        return ResponseEntity.ok(ApiResponse.success("权限保存成功", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
