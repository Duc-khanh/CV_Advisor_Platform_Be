package com.example.cvadvisorplatform.dto;

import lombok.Data;

@Data
public class AdminUserCreateRequest {
    private String fullName;
    private String email;
    private String password;
    private String role;
}
