package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.CvRequest;
import com.example.cvadvisorplatform.dto.CvResponse;
import com.example.cvadvisorplatform.model.CV;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.CVRepository;
import com.example.cvadvisorplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCvService {

    private final CVRepository cvRepository;
    private final UserRepository userRepository;

    public CvResponse saveCv(Long userId, CvRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CV cv = new CV();
        cv.setUser(user);
        cv.setFileName(request.getFileName() != null ? request.getFileName() : "Untitled CV");
        cv.setCvText(request.getCvText());
        cv.setCreatedAt(LocalDateTime.now());

        CV savedCv = cvRepository.save(cv);
        return mapToResponse(savedCv);
    }

    public CvResponse updateCv(Long userId, Long cvId, CvRequest request) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new RuntimeException("CV not found"));

        if (!cv.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("You do not have permission to update this CV");
        }

        cv.setFileName(request.getFileName() != null ? request.getFileName() : cv.getFileName());
        if (request.getCvText() != null) {
            cv.setCvText(request.getCvText());
        }

        CV updatedCv = cvRepository.save(cv);
        return mapToResponse(updatedCv);
    }

    public List<CvResponse> getAllCvsByUser(Long userId) {
        List<CV> cvs = cvRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId);
        return cvs.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public CvResponse getCvById(Long userId, Long cvId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new RuntimeException("CV not found"));

        if (!cv.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("You do not have permission to view this CV");
        }

        return mapToResponse(cv);
    }

    public void deleteCv(Long userId, Long cvId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new RuntimeException("CV not found"));

        if (!cv.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("You do not have permission to delete this CV");
        }

        cvRepository.delete(cv);
    }

    private CvResponse mapToResponse(CV cv) {
        CvResponse response = new CvResponse();
        response.setCvId(cv.getCvId());
        response.setFileName(cv.getFileName());
        response.setCvText(cv.getCvText());
        response.setCreatedAt(cv.getCreatedAt());
        return response;
    }
}
