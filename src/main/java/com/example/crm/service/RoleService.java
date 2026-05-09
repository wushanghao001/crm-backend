
package com.example.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crm.annotation.OperationLog;
import com.example.crm.dto.PageResponse;
import com.example.crm.entity.Role;
import com.example.crm.entity.User;
import com.example.crm.mapper.RoleMapper;
import com.example.crm.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoleService {

    private final RoleMapper roleMapper;
    private final UserMapper userMapper;

    public RoleService(RoleMapper roleMapper, UserMapper userMapper) {
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
    }

    public PageResponse<Role> listRoles(Integer pageNum, Integer pageSize, String name) {
        Page<Role> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();

        if (name != null && !name.isEmpty()) {
            queryWrapper.like(Role::getName, name);
        }

        IPage<Role> result = roleMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public Role getRoleById(Long id) {
        return roleMapper.selectById(id);
    }

    @OperationLog(module = "权限管理", type = "新增", content = "创建角色")
    public Role createRole(Role role) {
        if (roleMapper.findByName(role.getName()) != null) {
            throw new IllegalArgumentException("角色名称已存在");
        }

        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());

        roleMapper.insert(role);
        return role;
    }

    @OperationLog(module = "权限管理", type = "编辑", content = "更新角色信息")
    public Role updateRole(Long id, Role role) {
        Role existing = roleMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("角色不存在");
        }

        existing.setName(role.getName());
        existing.setDescription(role.getDescription());
        existing.setPermissions(role.getPermissions());
        existing.setUpdatedAt(LocalDateTime.now());

        roleMapper.updateById(existing);
        return existing;
    }

    @OperationLog(module = "权限管理", type = "删除", content = "删除角色")
    public void deleteRole(Long id) {
        if (roleMapper.selectById(id) == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        roleMapper.deleteById(id);
    }

    public List<Role> getAllRoles() {
        return roleMapper.selectList(null);
    }

    @OperationLog(module = "权限管理", type = "权限分配", content = "分配角色权限")
    public void saveRolePermissions(Long roleId, List<String> permissions) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        String permStr = permissions != null ? String.join(",", permissions) : "";
        role.setPermissions(permStr);
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(role);

        LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(User::getRoleId, roleId);
        List<User> roleUsers = userMapper.selectList(userQuery);
        for (User user : roleUsers) {
            user.setPermissions(permStr);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }
}
