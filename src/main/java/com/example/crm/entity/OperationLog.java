package com.example.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String operator;

    private String operatorId;

    private String type;

    private String content;

    private String ip;

    private String params;

    private String result;

    private Integer status;

    private String errorMessage;

    private String module;

    private Long targetId;

    private String targetName;

    private String userAgent;

    private LocalDateTime createdAt;
}
