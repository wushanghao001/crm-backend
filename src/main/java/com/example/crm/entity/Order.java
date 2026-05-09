package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private String contactName;

    private String phone;

    private String email;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal paidAmount;

    private String status;

    private String payStatus;

    private String paymentMethod;

    private LocalDateTime paidAt;

    private String transactionNo;

    private String remark;

    private Long creatorId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}