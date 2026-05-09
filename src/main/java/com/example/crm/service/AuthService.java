
package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.crm.dto.LoginRequest;
import com.example.crm.dto.LoginResponse;
import com.example.crm.dto.RegisterRequest;
import com.example.crm.dto.UserResponse;
import com.example.crm.entity.User;
import com.example.crm.entity.VerificationCode;
import com.example.crm.mapper.RoleMapper;
import com.example.crm.mapper.UserMapper;
import com.example.crm.mapper.VerificationCodeMapper;
import com.example.crm.util.JwtUtil;
import com.example.crm.util.PasswordUtil;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final VerificationCodeMapper verificationCodeMapper;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    private static final SecureRandom random = new SecureRandom();

    public AuthService(UserMapper userMapper, RoleMapper roleMapper, VerificationCodeMapper verificationCodeMapper,
                      PasswordUtil passwordUtil, JwtUtil jwtUtil, EmailService emailService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.verificationCodeMapper = verificationCodeMapper;
        this.passwordUtil = passwordUtil;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            System.out.println("=== AuthService.login() called with username: " + request.getUsername());
            
            System.out.println("=== Step 1: Querying user from database...");
            User user = userMapper.findByUsername(request.getUsername());
            System.out.println("=== Step 1 completed: User found: " + (user != null ? user.getUsername() : "null"));
            
            if (user == null) {
                System.out.println("=== User not found: " + request.getUsername());
                throw new IllegalArgumentException("用户名或密码错误");
            }
            
            System.out.println("=== Step 2: Checking password...");
            boolean passwordMatch = passwordUtil.matches(request.getPassword(), user.getPassword());
            System.out.println("=== Step 2 completed: Password match: " + passwordMatch);
            
            if (!passwordMatch) {
                System.out.println("=== Password does not match for user: " + request.getUsername());
                throw new IllegalArgumentException("用户名或密码错误");
            }
            
            System.out.println("=== Step 3: Generating JWT token...");
            String token = jwtUtil.generateToken(user.getUsername());
            System.out.println("=== Step 3 completed: Token generated successfully");
            
            System.out.println("=== Step 4: Converting to UserResponse...");
            UserResponse userResponse = convertToUserResponse(user);
            System.out.println("=== Step 4 completed");
            
            return new LoginResponse(token, userResponse);
        } catch (Exception e) {
            System.out.println("=== Exception in AuthService.login(): " + e.getClass().getName());
            System.out.println("=== Exception message: " + (e.getMessage() != null ? e.getMessage() : "null"));
            e.printStackTrace();
            throw e;
        }
    }

    public UserResponse register(RegisterRequest request) {
        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        if (userMapper.findByEmail(request.getEmail()) != null) {
            throw new IllegalArgumentException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordUtil.encode(request.getPassword()));
        user.setRole("user");

        var userRole = roleMapper.findByName("user");
        if (userRole != null) {
            user.setRoleId(userRole.getId());
            user.setPermissions(userRole.getPermissions());
        } else {
            user.setPermissions("customer:view,customer:edit");
        }

        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);

        return convertToUserResponse(user);
    }

    public void sendVerificationCode(String email, String type) {
        User existUser = userMapper.findByEmail(email);
        if ("register".equals(type)) {
            if (existUser != null) {
                throw new IllegalArgumentException("该邮箱已被注册");
            }
        } else if ("forgot_password".equals(type)) {
            if (existUser == null) {
                throw new IllegalArgumentException("该邮箱未注册");
            }
        }

        LambdaQueryWrapper<VerificationCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VerificationCode::getEmail, email)
                   .eq(VerificationCode::getType, type)
                   .eq(VerificationCode::getStatus, 1);
        verificationCodeMapper.delete(queryWrapper);

        String code = String.format("%06d", random.nextInt(1000000));

        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setType(type);
        verificationCode.setStatus(1);
        verificationCode.setCreatedAt(LocalDateTime.now());
        verificationCode.setUpdatedAt(LocalDateTime.now());
        verificationCodeMapper.insert(verificationCode);

        emailService.sendVerificationCode(email, code);
    }

    public void resetPassword(String email, String code, String newPassword) {
        LambdaQueryWrapper<VerificationCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VerificationCode::getEmail, email)
                   .eq(VerificationCode::getCode, code)
                   .eq(VerificationCode::getType, "forgot_password")
                   .eq(VerificationCode::getStatus, 1);
        VerificationCode verificationCode = verificationCodeMapper.selectOne(queryWrapper);

        if (verificationCode == null) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }

        if (verificationCode.getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("验证码已过期");
        }

        verificationCode.setStatus(0);
        verificationCodeMapper.updateById(verificationCode);

        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        user.setPassword(passwordUtil.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return convertToUserResponse(user);
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());

        if ("admin".equals(user.getRole())) {
            response.setPermissions(new String[]{"customer:view", "customer:add", "customer:edit", "customer:delete",
                "contact:view", "contact:add", "contact:edit", "contact:delete",
                "interaction:view", "interaction:add", "interaction:edit", "interaction:delete",
                "order:view", "order:add", "order:edit", "order:delete",
                "churn:view", "opportunity:view", "service:view", "product:view",
                "statistics:view", "user:view", "role:view", "log:view"});
        } else {
            String permissions = user.getPermissions();
            if (permissions != null && !permissions.isEmpty()) {
                response.setPermissions(permissions.split(","));
            } else {
                response.setPermissions(new String[]{});
            }
        }

        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
