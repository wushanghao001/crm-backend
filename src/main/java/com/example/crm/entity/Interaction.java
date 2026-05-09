package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interaction")
public class Interaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private Long contactId;

    private String type;

    private String content;

    private LocalDateTime interactionTime;

    private Long operatorId;

    private String operator;

    private String status;

    private String phone;

    private String email;

    private Long creatorId;

    private LocalDateTime createdAt;
}