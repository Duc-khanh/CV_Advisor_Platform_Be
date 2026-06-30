package com.example.cvadvisorplatform.dto;

import lombok.Data;

@Data
public class AdminUserUpdateRequest {
    private String fullName;
    private String email;
    private String role;
    private String password;
    private Long companyId;
}


