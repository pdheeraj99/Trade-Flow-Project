package com.tradeflow.auth.config;

import com.tradeflow.auth.entity.Role;
import com.tradeflow.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Initializes default roles on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        initializeRoles();
    }

    private void initializeRoles() {
        // Create USER role if not exists
        if (!roleRepository.existsByName(Role.USER)) {
            Role userRole = Objects.requireNonNull(Role.builder()
                    .name(Role.USER)
                    .description("Standard user role with trading permissions")
                    .build(), "USER role builder returned null");
            roleRepository.save(userRole);
            log.info("Created default role: {}", Role.USER);
        }

        // Create ADMIN role if not exists
        if (!roleRepository.existsByName(Role.ADMIN)) {
            Role adminRole = Objects.requireNonNull(Role.builder()
                    .name(Role.ADMIN)
                    .description("Administrator role with full system access")
                    .build(), "ADMIN role builder returned null");
            roleRepository.save(adminRole);
            log.info("Created default role: {}", Role.ADMIN);
        }
    }
}
