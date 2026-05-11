package com.example.cvadvisorplatform.controller;

import com.example.cvadvisorplatform.dto.AuthResponse;
import com.example.cvadvisorplatform.dto.LoginRequest;
import com.example.cvadvisorplatform.dto.RegisterRequest;
import com.example.cvadvisorplatform.dto.RegisterHrRequest;
import com.example.cvadvisorplatform.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().body("Register success");
    }

    @PostMapping("/register-hr")
    public ResponseEntity<?> registerHr(@RequestBody RegisterHrRequest request) {
        authService.registerHr(request);
        return ResponseEntity.ok().body("Register HR success");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
