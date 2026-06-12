package com.pasteleria.pos.config;

import com.pasteleria.pos.domain.entity.User;
import com.pasteleria.pos.domain.enums.UserRole;
import com.pasteleria.pos.repository.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final UUID SUPER_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean resetAdminPassword;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.reset-password:false}") boolean resetAdminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.resetAdminPassword = resetAdminPassword;
    }

    @Override
    public void run(String... args) {
        userRepository.findByUsername(DEFAULT_ADMIN_USERNAME).ifPresentOrElse(
                admin -> {
                    if (resetAdminPassword) {
                        admin.setPasswordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
                        admin.setActive(true);
                        userRepository.save(admin);
                    }
                },
                () -> {
                    User admin = new User();
                    admin.setId(SUPER_ADMIN_ID);
                    admin.setName("Super Admin");
                    admin.setUsername(DEFAULT_ADMIN_USERNAME);
                    admin.setPasswordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
                    admin.setRole(UserRole.SUPER_ADMIN);
                    admin.setActive(true);
                    admin.setBirthDate(LocalDate.of(1990, 1, 1));
                    userRepository.save(admin);
                });
    }
}
