package com.example.cvadvisorplatform.dto;

import lombok.Data;

@Data
public class AdminUserResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private String avatar;
    private String avatarUrl;
    private boolean enabled;
    private String hrApprovalStatus;
    private String companyName;
    private Long companyId;
}
