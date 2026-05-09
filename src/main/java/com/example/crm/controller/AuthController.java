package com.example.crm.controller;

import com.example.crm.dto.*;
import com.example.crm.entity.User;
import com.example.crm.mapper.UserMapper;
import com.example.crm.service.AuthService;
import com.example.crm.service.EmailService;
import com.example.crm.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final MenuService menuService;
    private final EmailService emailService;
    private final UserMapper userMapper;

    public AuthController(AuthService authService, MenuService menuService, EmailService emailService, UserMapper userMapper) {
        this.authService = authService;
        this.menuService = menuService;
        this.emailService = emailService;
        this.userMapper = userMapper;
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        System.out.println("=== Test endpoint called ===");
        return ResponseEntity.ok(ApiResponse.success("Server is running!"));
    }

    @GetMapping("/test-db")
    public ResponseEntity<ApiResponse<String>> testDb() {
        try {
            System.out.println("=== Test DB endpoint called ===");
            User user = userMapper.findByUsername("admin");
            System.out.println("=== DB Query Result: " + (user != null ? "User found" : "User not found"));
            if (user != null) {
                System.out.println("=== Username: " + user.getUsername());
                System.out.println("=== Password: " + user.getPassword());
            }
            return ResponseEntity.ok(ApiResponse.success("Database connection test completed"));
        } catch (Exception e) {
            System.out.println("=== DB Test Exception: " + e.getClass().getName());
            System.out.println("=== DB Test Exception Message: " + (e.getMessage() != null ? e.getMessage() : "null"));
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error(500, "Database connection failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        System.out.println("=== Login endpoint called with username: " + request.getUsername());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("注册成功", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("=== AuthController.getCurrentUser() ===");
        System.out.println("Authentication: " + authentication);
        
        if (authentication != null) {
            System.out.println("Principal class: " + authentication.getPrincipal().getClass().getName());
            System.out.println("Principal: " + authentication.getPrincipal());
        }

        if (authentication != null && authentication.getPrincipal() instanceof com.example.crm.entity.User user) {
            System.out.println("User found in principal: " + user.getUsername());
            UserResponse userResponse = authService.getUserByUsername(user.getUsername());
            System.out.println("UserResponse: " + userResponse);

            Map<String, Object> result = new HashMap<>();
            result.put("user", userResponse);
            result.put("menus", menuService.getMenus(user.getRole()));

            return ResponseEntity.ok(ApiResponse.success(result));
        }

        return ResponseEntity.ok(ApiResponse.error(401, "未登录"));
    }

    @GetMapping("/menus")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getMenus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof com.example.crm.entity.User user) {
            return ResponseEntity.ok(ApiResponse.success(menuService.getMenus(user.getRole())));
        }

        return ResponseEntity.ok(ApiResponse.error(401, "未登录"));
    }

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<String>> sendCode(@RequestBody SendCodeRequest request) {
        authService.sendVerificationCode(request.getEmail(), request.getType());
        return ResponseEntity.ok(ApiResponse.success("验证码已发送"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("密码重置成功"));
    }
}