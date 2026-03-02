package com.example.cvadvisorplatform.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeJobImage(MultipartFile file);
}