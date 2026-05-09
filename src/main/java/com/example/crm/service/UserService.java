
package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.annotation.OperationLog;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.User;
import com.example.crm.mapper.UserMapper;
import com.example.crm.entity.Role;
import com.example.crm.mapper.RoleMapper;
import com.example.crm.util.PasswordUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordUtil passwordUtil;

    public UserService(UserMapper userMapper, RoleMapper roleMapper, PasswordUtil passwordUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordUtil = passwordUtil;
    }

    public PageResponse<User> listUsers(Integer pageNum, Integer pageSize, String username, String role, String status) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        if (username != null && !username.isEmpty()) {
            queryWrapper.like(User::getUsername, username);
        }
        if (role != null && !role.isEmpty()) {
            queryWrapper.eq(User::getRole, role);
        }
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(User::getStatus, status);
        }

        IPage<User> result = userMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @OperationLog(module = "用户管理", type = "新增", content = "创建用户")
    public User createUser(User user) {
        if (userMapper.findByUsername(user.getUsername()) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        if (user.getRoleId() != null) {
            Role role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                user.setRole(role.getName());
                user.setPermissions(role.getPermissions());
            }
        }

        if (user.getUserType() == null) {
            user.setUserType("normal");
        }

        user.setPassword(passwordUtil.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);
        return user;
    }

    @OperationLog(module = "用户管理", type = "编辑", content = "更新用户信息")
    public User updateUser(Long id, User user) {
        User existingUser = userMapper.selectById(id);
        if (existingUser == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        
        if (user.getUsername() != null && !user.getUsername().equals(existingUser.getUsername())) {
            if (userMapper.findByUsername(user.getUsername()) != null) {
                throw new IllegalArgumentException("用户名已存在");
            }
            existingUser.setUsername(user.getUsername());
        }
        
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getPhone() != null) {
            existingUser.setPhone(user.getPhone());
        }
        if (user.getRoleId() != null) {
            existingUser.setRoleId(user.getRoleId());
            Role role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                existingUser.setRole(role.getName());
                existingUser.setPermissions(role.getPermissions());
            }
        }
        if (user.getRole() != null) {
            existingUser.setRole(user.getRole());
        }
        if (user.getUserType() != null) {
            existingUser.setUserType(user.getUserType());
        }
        if (user.getPermissions() != null) {
            existingUser.setPermissions(user.getPermissions());
        }
        if (user.getStatus() != null) {
            existingUser.setStatus(user.getStatus());
        }
        
        existingUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(existingUser);
        
        return existingUser;
    }

    @OperationLog(module = "用户管理", type = "删除", content = "删除用户")
    public void deleteUser(Long id) {
        if (userMapper.selectById(id) == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        userMapper.deleteById(id);
    }

    @OperationLog(module = "用户管理", type = "重置密码", content = "重置用户密码")
    public void resetPassword(Long id, String password) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setPassword(passwordUtil.encode(password));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @OperationLog(module = "用户管理", type = "分配角色", content = "分配用户角色")
    public Map<String, Object> assignRole(Long userId, Long roleId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        
        user.setRoleId(roleId);
        user.setRole(role.getName());
        user.setPermissions(role.getPermissions());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("role", role);
        result.put("permissions", role.getPermissions() != null ? role.getPermissions().split(",") : new String[]{});
        
        return result;
    }
}
