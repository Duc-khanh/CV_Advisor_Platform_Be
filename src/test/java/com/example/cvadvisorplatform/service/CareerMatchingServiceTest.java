package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.model.Job;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class CareerMatchingServiceTest {
    private final CareerMatchingService service = new CareerMatchingService(
            Mockito.mock(JobRepository.class), new ObjectMapper());

    @Test
    void separatesMatchedAndNotMentionedSkills() {
        User user = new User();
        Job job = new Job();
        job.setRequiredSkills(List.of("React", "TypeScript", "Next.js", "REST API"));
        String cvText = "React TypeScript REST API developer";

        CareerMatchingService.MatchResult result = service.compare(user, cvText, job);

        assertThat(result.matchedSkills()).containsExactly("React", "TypeScript", "REST API");
        assertThat(result.missingSkills()).containsExactly("Next.js");
        assertThat(result.score()).isEqualTo(75);
    }
}
