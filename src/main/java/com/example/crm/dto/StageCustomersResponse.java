package com.example.crm.dto;

import lombok.Data;

import java.util.List;

@Data
public class StageCustomersResponse {
    private List<CustomerBrief> customers;
    private Integer total;
}