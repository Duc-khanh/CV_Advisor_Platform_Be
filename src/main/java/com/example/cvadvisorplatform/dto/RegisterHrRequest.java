package com.example.cvadvisorplatform.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;

@Getter
@Setter
public class RegisterHrRequest {
    @NotBlank @Size(max = 120) private String fullName;
    @NotBlank @Email @Size(max = 255) private String email;
    @NotBlank @Size(min = 6, max = 72) private String password;
    
    // Company Information
    @NotBlank @Size(max = 200) private String companyName;
    @Size(max = 120) private String industryName;
    private String address;
    private String description;
}
