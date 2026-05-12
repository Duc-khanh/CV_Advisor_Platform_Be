package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.LoginRequest;
import com.example.cvadvisorplatform.dto.RegisterRequest;
import com.example.cvadvisorplatform.model.Role;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.RoleRepository;
import com.example.cvadvisorplatform.repository.UserRepository;
import com.example.cvadvisorplatform.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.cvadvisorplatform.dto.RegisterHrRequest;
import com.example.cvadvisorplatform.model.Company;
import com.example.cvadvisorplatform.model.Industry;
import com.example.cvadvisorplatform.repository.CompanyRepository;
import com.example.cvadvisorplatform.repository.IndustryRepository;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CompanyRepository companyRepository;
    private final IndustryRepository industryRepository;


    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Role role = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER not found"));

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        userRepository.save(user);
    }

    public void registerHr(RegisterHrRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Role role = roleRepository.findByRoleName("HR")
                .orElseThrow(() -> new RuntimeException("Role HR not found"));

        Industry industry = industryRepository.findByIndustryName(request.getIndustryName())
                .orElseGet(() -> {
                    Industry newIndustry = new Industry();
                    newIndustry.setIndustryName(request.getIndustryName());
                    return industryRepository.save(newIndustry);
                });

        Company company = new Company();
        company.setCompanyName(request.getCompanyName());
        company.setIndustry(industry);
        company.setAddress(request.getAddress());
        company.setDescription(request.getDescription());
        company = companyRepository.save(company);

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setCompany(company);

        userRepository.save(user);
    }

    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.");
        }

        return jwtService.generateToken(user);
    }

    public String loginWithGoogle(String accessToken) throws Exception {
        // Gọi Google UserInfo API để lấy thông tin người dùng từ access_token
        // (Frontend dùng useGoogleLogin implicit flow → trả về access_token, không phải id_token)
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid Google access token: " + e.getMessage());
        }

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Failed to fetch Google user info");
        }

        Map<String, Object> userInfo = response.getBody();
        String email = (String) userInfo.get("email");
        String name  = (String) userInfo.get("name");

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Google account does not have an email");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            Role role = roleRepository.findByRoleName("USER")
                    .orElseThrow(() -> new RuntimeException("Role USER not found"));

            user = new User();
            user.setEmail(email);
            user.setFullName(name != null ? name : "Google User");
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setRole(role);
            user = userRepository.save(user);
        } else if (!user.isEnabled()) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.");
        }

        return jwtService.generateToken(user);
    }
}

