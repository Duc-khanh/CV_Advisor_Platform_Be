package com.example.cvadvisorplatform.dto;

import lombok.Data;

@Data
public class CurrentUserUpdateRequest {
    private String fullName;
    private String email;
}