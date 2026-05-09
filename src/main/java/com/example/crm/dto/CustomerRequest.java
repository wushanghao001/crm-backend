
package com.example.crm.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerRequest {

    private String name;

    private String phone;

    private String email;

    private String address;

    private String industry;

    private String scale;

    private String source;

    private String status;

    private String churnReason;

    private String customerLevel;

    private BigDecimal totalAmount;
}
