package com.pos.identity_service.config;

import com.pos.identity_service.model.Role;
import com.pos.identity_service.model.User;
import com.pos.identity_service.repository.RoleRepository;
import com.pos.identity_service.repository.UserRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "bootstrap.admin.enabled", havingValue = "true")
public class AdminBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapper.class);

    private final BootstrapAdminProperties props;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapper(BootstrapAdminProperties props,
                             UserRepository userRepository,
                             RoleRepository roleRepository,
                             PasswordEncoder passwordEncoder) {
        this.props = props;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = (props.getUsername() == null) ? "" : props.getUsername().trim();
        if (username.isEmpty()) {
            throw new IllegalStateException("bootstrap.admin.username must be set when bootstrap.admin.enabled=true");
        }

        String password = props.getPassword();
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("bootstrap.admin.password must be set when bootstrap.admin.enabled=true");
        }

        com.pos.identity_service.service.PasswordPolicy.validateOrThrow(username, password);

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ADMIN");
                    role.setDescription("System administrator");
                    return roleRepository.save(role);
                });

        userRepository.findByUsername(username).ifPresentOrElse(
                existing -> log.info("Admin bootstrap skipped: user '{}' already exists (id={})", existing.getUsername(), existing.getId()),
                () -> {
                    User admin = new User();
                    admin.setUsername(username);
                    admin.setPasswordHash(passwordEncoder.encode(password));
                    admin.setRole(adminRole);
                    admin.setAdminUnitId(props.getAdminUnitId());
                    admin.setStatus("ACTIVE");
                    admin.setCreatedAt(Instant.now());
                    User saved = userRepository.save(admin);
                    log.info("Admin bootstrap created user '{}' (id={})", saved.getUsername(), saved.getId());
                }
        );
    }
}

