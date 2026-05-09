
package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String phone;

    private String email;

    private String address;

    private String industry;

    private String scale;

    private String source;

    private String status;

    private String churnReason;

    private BigDecimal totalAmount;

    private LocalDateTime lastContactTime;

    private LocalDateTime lastFollowTime;

    private Integer followCount;

    private String customerLevel;

    private Long creatorId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
