package com.example.cvadvisorplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final CloudinaryService cloudinaryService;
    @Value("${cv.application-upload-dir:private-uploads/applications}")
    private String applicationUploadDir;

    @Override
    public String storeJobImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return cloudinaryService.uploadFile(file, "cv_platform/jobs");
    }

    @Override
    public String storeArticleImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return cloudinaryService.uploadFile(file, "cv_platform/articles");
    }

    @Override
    public String storeCvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            return storeCvFile(file.getBytes(), file.getOriginalFilename());
        } catch (IOException ex) {
            throw new RuntimeException("Cannot store CV file", ex);
        }
    }

    @Override
    public String storeCvFile(byte[] content, String fileName) {
        if (content == null || content.length == 0) {
            return null;
        }
        String safeName = fileName == null ? "resume.pdf" : Paths.get(fileName).getFileName().toString();
        String extension = safeName.contains(".") ? safeName.substring(safeName.lastIndexOf('.')) : "";
        String storedName = UUID.randomUUID() + extension.toLowerCase(Locale.ROOT);
        Path directory = Paths.get(applicationUploadDir).toAbsolutePath().normalize();
        Path target = directory.resolve(storedName).normalize();
        if (!target.startsWith(directory)) throw new RuntimeException("Invalid CV path");
        try {
            Files.createDirectories(directory);
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
            return "private:" + storedName;
        } catch (IOException ex) {
            throw new RuntimeException("Cannot store CV file", ex);
        }
    }

    @Override
    public InputStream openCvFile(String reference) {
        if (reference == null || reference.isBlank()) throw new RuntimeException("CV file not found");
        if (!reference.startsWith("private:")) return cloudinaryService.downloadFileAsStream(reference);
        Path directory = Paths.get(applicationUploadDir).toAbsolutePath().normalize();
        Path file = directory.resolve(reference.substring("private:".length())).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) throw new RuntimeException("CV file not found");
        try {
            return Files.newInputStream(file);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read CV file", ex);
        }
    }

    @Override
    public String cvFileName(String reference) {
        if (reference == null || !reference.startsWith("private:")) return "resume";
        return Paths.get(reference.substring("private:".length())).getFileName().toString();
    }
}
