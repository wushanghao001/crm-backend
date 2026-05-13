package com.example.crm.controller;

import com.example.crm.dto.*;
import com.example.crm.entity.User;
import com.example.crm.mapper.UserMapper;
import com.example.crm.service.AuthService;
import com.example.crm.service.EmailService;
import com.example.crm.service.MenuService;
import com.example.crm.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, MenuService menuService, EmailService emailService,
                          UserMapper userMapper, JwtUtil jwtUtil) {
        this.authService = authService;
        this.menuService = menuService;
        this.emailService = emailService;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
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
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        System.out.println("=== Login endpoint called with username: " + request.getUsername());

        String loginIp = getClientIp(httpRequest);
        String deviceInfo = getDeviceInfo(httpRequest);

        LoginResponse response = authService.login(request, loginIp, deviceInfo);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest httpRequest) {
        String token = extractToken(httpRequest);
        Long userId = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            userId = user.getId();
            authService.logout(userId);
        } else if (token != null) {
            try {
                String username = jwtUtil.extractUsername(token);
                if (username != null) {
                    User user = userMapper.findByUsername(username);
                    if (user != null) {
                        userId = user.getId();
                        authService.logout(userId);
                    }
                }
            } catch (Exception e) {
                System.out.println("=== Logout: Failed to extract user from token: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(ApiResponse.success("退出登录成功"));
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

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String getDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "unknown";
        }
        if (userAgent.contains("Windows")) {
            return "Windows PC";
        } else if (userAgent.contains("Macintosh")) {
            return "Mac";
        } else if (userAgent.contains("iPhone")) {
            return "iPhone";
        } else if (userAgent.contains("iPad")) {
            return "iPad";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else {
            return "Other";
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
