package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.model.Company;
import com.example.cvadvisorplatform.model.Job;
import com.example.cvadvisorplatform.model.JobApplication;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.JobApplicationRepository;
import com.example.cvadvisorplatform.repository.JobRepository;
import com.example.cvadvisorplatform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobApplicationServiceTest {

    private JobApplicationRepository applicationRepository;
    private FileStorageService fileStorageService;
    private UserCvService userCvService;
    private FileValidationService fileValidationService;
    private JobApplicationService service;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(JobApplicationRepository.class);
        JobRepository jobRepository = mock(JobRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MailService mailService = mock(MailService.class);
        fileStorageService = mock(FileStorageService.class);
        userCvService = mock(UserCvService.class);
        fileValidationService = mock(FileValidationService.class);
        service = new JobApplicationService(
                applicationRepository,
                jobRepository,
                userRepository,
                mailService,
                fileStorageService,
                userCvService,
                fileValidationService
        );

        Company company = new Company();
        company.setCompanyName("Example Company");
        Job job = new Job();
        job.setTitle("Backend Developer");
        job.setCompany(company);
        User user = new User();
        user.setEmail("candidate@example.com");
        user.setFullName("Candidate");

        when(applicationRepository.existsByUser_UserIdAndJob_JobId(1L, 2L)).thenReturn(false);
        when(jobRepository.findById(2L)).thenReturn(Optional.of(job));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void appliesWithAnOwnedSavedCv() throws Exception {
        byte[] content = "saved cv".getBytes();
        when(userCvService.getCvFile(1L, 9L)).thenReturn(new UserCvService.CvFile(
                new ByteArrayResource(content),
                "resume.pdf",
                "application/pdf",
                content.length
        ));
        when(fileStorageService.storeCvFile(content, "resume.pdf"))
                .thenReturn("https://files.example/resume.pdf");

        service.apply(1L, 2L, null, 9L);

        verify(userCvService).getCvFile(1L, 9L);
        verify(fileStorageService).storeCvFile(content, "resume.pdf");
        verify(applicationRepository).save(any(JobApplication.class));
    }

    @Test
    void appliesWithANewUploadedCv() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "cv", "resume.pdf", "application/pdf", "new cv".getBytes());
        when(fileStorageService.storeCvFile(file)).thenReturn("https://files.example/new.pdf");

        service.apply(1L, 2L, file, null);

        verify(fileStorageService).storeCvFile(file);
        verify(userCvService, never()).getCvFile(any(), any());
        verify(applicationRepository).save(any(JobApplication.class));
    }

    @Test
    void rejectsWhenBothCvSourcesAreProvided() {
        MockMultipartFile file = new MockMultipartFile(
                "cv", "resume.pdf", "application/pdf", "new cv".getBytes());

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> service.apply(1L, 2L, file, 9L)
        );

        assertEquals("Vui lòng chọn một CV đã lưu hoặc tải lên một file CV mới", error.getMessage());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void rejectsWhenNoCvSourceIsProvided() {
        assertThrows(RuntimeException.class, () -> service.apply(1L, 2L, null, null));
        verify(applicationRepository, never()).save(any());
    }
}
