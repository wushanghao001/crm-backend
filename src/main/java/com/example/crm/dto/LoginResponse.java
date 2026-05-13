package com.example.crm.dto;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;

    private UserResponse user;

    private Boolean hasExistingSession;

    public LoginResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
        this.hasExistingSession = false;
    }

    public LoginResponse(String token, UserResponse user, Boolean hasExistingSession) {
        this.token = token;
        this.user = user;
        this.hasExistingSession = hasExistingSession;
    }
}
