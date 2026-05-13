package com.example.crm.controller;

import com.example.crm.dto.ApiResponse;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.User;
import com.example.crm.entity.UserSession;
import com.example.crm.service.AuthService;
import com.example.crm.service.SessionService;
import com.example.crm.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final SessionService sessionService;
    private final AuthService authService;

    public UserController(UserService userService, SessionService sessionService, AuthService authService) {
        this.userService = userService;
        this.sessionService = sessionService;
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<PageResponse<User>>> listUsers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {

        PageResponse<User> response = userService.listUsers(pageNum, pageSize, username, role, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.ok(ApiResponse.success("创建成功", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long id, @RequestBody User user) {
        User updated = userService.updateUser(id, user);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PutMapping("/{id}/reset")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String password = body.get("password");
        userService.resetPassword(id, password);
        return ResponseEntity.ok(ApiResponse.success("密码重置成功", null));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> assignRole(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long roleId = ((Number) body.get("roleId")).longValue();
        Map<String, Object> result = userService.assignRole(id, roleId);
        return ResponseEntity.ok(ApiResponse.success("角色分配成功", result));
    }

    @GetMapping("/online")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<List<UserSession>>> getOnlineUsers() {
        List<UserSession> sessions = sessionService.getAllActiveSessions();
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping("/session/{userId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<ApiResponse<String>> forceLogoutUser(@PathVariable Long userId) {
        sessionService.deactivateUserSessions(userId);
        return ResponseEntity.ok(ApiResponse.success("已将用户踢出登录", null));
    }
}
