package com.tradeflow.auth.config;

import com.tradeflow.auth.entity.Role;
import com.tradeflow.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
            roleRepository.save(Role.builder()
                    .name(Role.USER)
                    .description("Standard user role with trading permissions")
                    .build());
            log.info("Created default role: {}", Role.USER);
        }

        // Create ADMIN role if not exists
        if (!roleRepository.existsByName(Role.ADMIN)) {
            roleRepository.save(Role.builder()
                    .name(Role.ADMIN)
                    .description("Administrator role with full system access")
                    .build());
            log.info("Created default role: {}", Role.ADMIN);
        }
    }
}
