package com.example.crm.dto;

import lombok.Data;

@Data
public class CustomerBrief {
    private Long id;
    private String name;
    private String phone;
    private String industry;
    private String status;
}