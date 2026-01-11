package com.example.cvadvisorplatform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class UserController {
    @GetMapping("/home")
    public String home() {
        return "USER HOME PUBLIC";
    }
}
