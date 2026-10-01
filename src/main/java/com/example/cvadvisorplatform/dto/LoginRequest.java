package com.example.cvadvisorplatform.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;

@Getter
@Setter
public class LoginRequest {
    @NotBlank @Email private String email;
    @NotBlank @Size(max = 72) private String password;
}

