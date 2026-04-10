package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.dto.JobPublicResponse;
import com.example.cvadvisorplatform.service.HrJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class UserController {

    private final HrJobService jobService;
}
