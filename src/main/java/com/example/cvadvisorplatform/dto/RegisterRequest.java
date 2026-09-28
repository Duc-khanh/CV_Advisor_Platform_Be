package com.example.cvadvisorplatform.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank @Size(max = 120) private String fullName;
    @NotBlank @Email @Size(max = 255) private String email;
    @NotBlank @Size(min = 8, max = 72) private String password;
}
