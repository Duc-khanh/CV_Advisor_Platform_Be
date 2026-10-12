package com.example.cvadvisorplatform.config;

import com.example.cvadvisorplatform.model.Role;
import com.example.cvadvisorplatform.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
@RequiredArgsConstructor
public class RoleSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("USER");
        seed("HR");
        seed("ADMIN");
    }

    private void seed(String name) {
        if (roleRepository.findByRoleName(name).isPresent()) {
            return;
        }
        Role role = new Role();
        role.setRoleName(name);
        roleRepository.save(role);
    }
}
