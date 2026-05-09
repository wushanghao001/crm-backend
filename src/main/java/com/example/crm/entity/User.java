
package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String email;

    private String phone;

    private Long roleId;

    private String role;

    private String userType;

    private String permissions;

    private Integer status;

    private BigDecimal monthTarget;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
