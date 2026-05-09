package com.example.crm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InteractionDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long contactId;
    private String contactName;
    private String type;
    private String content;
    private LocalDateTime interactionTime;
    private String operator;
    private String status;
    private String phone;
    private String email;
    private LocalDateTime createdAt;
}