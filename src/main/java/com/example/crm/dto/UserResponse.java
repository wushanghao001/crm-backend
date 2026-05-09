
package com.example.crm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {

    private Long id;

    private String username;

    private String email;

    private String phone;

    private String role;

    private String[] permissions;

    private LocalDateTime createdAt;
}
