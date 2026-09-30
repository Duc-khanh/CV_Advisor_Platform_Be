package com.example.cvadvisorplatform.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface FileStorageService {
    String storeJobImage(MultipartFile file);
    String storeArticleImage(MultipartFile file);
    String storeCvFile(MultipartFile file);
    String storeCvFile(byte[] content, String fileName);
    InputStream openCvFile(String reference);
    String cvFileName(String reference);
}
