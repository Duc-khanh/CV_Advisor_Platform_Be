package com.example.cvadvisorplatform.dto;

import lombok.Data;
import java.util.List;

@Data
public class CurrentUserUpdateRequest {
    private String fullName;
    private String email;
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