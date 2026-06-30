package com.example.cvadvisorplatform.dto;

import lombok.Data;

@Data
public class AdminCompanyCreateRequest {
    private String companyName;
    private String industryName;
    private String address;
    private String description;
}
