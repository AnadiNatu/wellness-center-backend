package com.demo.wellness_center.config;

import com.demo.wellness_center.domain.enums.AuthProvider;
import com.demo.wellness_center.domain.enums.UserRole;
import com.demo.wellness_center.domain.models.User;
import com.demo.wellness_center.domain.port.UserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements ApplicationRunner {

    private final AdminProperties props;
    private final UserPort userPort;
    private final PasswordEncoder encoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!props.enabled()) return;
        if (isBlank(props.email()) || isBlank(props.password())) {
            log.warn("Admin not created: ADMIN_EMAIL and ADMIN_PASSWORD must be set");
            return;
        }

        String email = props.email().trim().toLowerCase();
        User admin = userPort.findByEmail(email).orElse(null);

        if (admin == null) {
            admin = new User();
            admin.setEmail(email);
            admin.setPassword(encoder.encode(props.password()));
            admin.setRole(UserRole.ADMIN);
            admin.setProvider(AuthProvider.LOCAL);
            admin.setEmailVerified(true);
            admin.setPhoneVerified(!isBlank(props.phoneNumber()));
            admin.setApproved(true);
            fillProfile(admin);
            userPort.save(admin);
            log.info("Admin account created: {}", email);
        } else if (props.forceUpdate()) {
            // Deliberate update only. The password is never overwritten on a normal restart.
            admin.setPassword(encoder.encode(props.password()));
            admin.setRole(UserRole.ADMIN);
            fillProfile(admin);
            userPort.save(admin);
            log.info("Admin account updated: {}", email);
        }
    }

    private void fillProfile(User a) {
        a.setFirstName(props.firstName());
        a.setLastName(props.lastName());
        a.setPhoneNumber(isBlank(props.phoneNumber()) ? null : props.phoneNumber().trim());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

@ConfigurationProperties(prefix = "app.admin")
record AdminProperties(boolean enabled, String firstName, String lastName,
                       String email, String phoneNumber, String password,
                       boolean forceUpdate) {}