package com.example.cvadvisorplatform.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterHrRequest {
    private String fullName;
    private String email;
    private String password;
    
    // Company Information
    private String companyName;
    private String industryName;
    private String address;
    private String description;
}
