package com.example.crm.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerFollowDTO {
    private Integer id;
    private Integer customerId;
    private String customerName;
    private String followType;
    private String followResult;
    private String intentLevel;
    private String content;
    private String remark;
    private LocalDateTime nextFollowTime;
    private String attachment;
    private Boolean remindFlag;
    private Integer followUserId;
    private String followUserName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String contactName;
    private BigDecimal totalPaidAmount;
    private String currentStage;
}