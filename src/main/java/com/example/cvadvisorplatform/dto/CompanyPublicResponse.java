package com.example.cvadvisorplatform.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyPublicResponse {
    private Long companyId;
    private String companyName;
    private String logoUrl;
    private String address;
    private String description;
    private String websiteUrl;
    private String email;
    private String phone;
    private double rating;
    private long jobCount;
}
