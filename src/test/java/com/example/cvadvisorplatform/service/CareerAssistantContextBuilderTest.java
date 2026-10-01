package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.CareerAssistantRequest;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.JobApplicationRepository;
import com.example.cvadvisorplatform.repository.JobRepository;
import com.example.cvadvisorplatform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CareerAssistantContextBuilderTest {
    @Test
    void rejectsApplicationOwnedByAnotherUser() {
        UserRepository users = mock(UserRepository.class);
        JobRepository jobs = mock(JobRepository.class);
        JobApplicationRepository applications = mock(JobApplicationRepository.class);
        User user = new User();
        user.setUserId(7L);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(applications.findByIdAndUser_UserId(99L, 7L)).thenReturn(Optional.empty());

        CareerAssistantRequest request = new CareerAssistantRequest();
        request.getContext().setApplicationId(99L);
        CareerAssistantContextBuilder builder =
                new CareerAssistantContextBuilder(users, jobs, applications);

        assertThatThrownBy(() -> builder.build(7L, request))
                .hasMessageContaining("thuộc tài khoản hiện tại");
    }
}
