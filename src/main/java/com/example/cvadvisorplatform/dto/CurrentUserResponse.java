package com.example.cvadvisorplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String role;
    private boolean enabled;
    private String avatarUrl;
    private String avatar;
    private String phone;
    private String headline;
    private String location;
    private String bio;
    private String birthday;
    private String gender;
    private String personalLink;
    private List<String> skills;
    private List<Object> education;
    private List<Object> experience;
    private List<Object> projects;
}
