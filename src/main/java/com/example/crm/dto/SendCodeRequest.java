package com.example.crm.dto;

import lombok.Data;

@Data
public class SendCodeRequest {
    private String email;
    private String type;
}
