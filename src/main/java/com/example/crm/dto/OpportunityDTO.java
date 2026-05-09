package com.example.crm.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OpportunityDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private String name;
    private String stage;
    private BigDecimal amount;
    private Double probability;
    private LocalDateTime expectedCloseDate;
    private String description;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}