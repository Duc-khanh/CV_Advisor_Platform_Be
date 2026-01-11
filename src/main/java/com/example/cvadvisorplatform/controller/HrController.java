package com.example.cvadvisorplatform.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr")
public class HrController {

    @GetMapping("/dashboard")
    public String hrDashboard() {
        return "HR DASHBOARD - ONLY HR ACCESS";
    }

    @GetMapping("/candidates")
    public String manageCandidates() {
        return "HR MANAGE CANDIDATES";
    }
}

