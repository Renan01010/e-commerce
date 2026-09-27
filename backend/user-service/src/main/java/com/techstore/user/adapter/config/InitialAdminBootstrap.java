package com.techstore.user.adapter.config;

import com.techstore.user.application.service.UserManagementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InitialAdminBootstrap implements ApplicationRunner {
    private final UserManagementService userManagementService;
    private final String initialAdminEmail;
    private final String initialAdminPassword;

    public InitialAdminBootstrap(
            UserManagementService userManagementService,
            @Value("${INITIAL_ADMIN_EMAIL:}") String initialAdminEmail,
            @Value("${INITIAL_ADMIN_PASSWORD:}") String initialAdminPassword) {
        this.userManagementService = userManagementService;
        this.initialAdminEmail = initialAdminEmail;
        this.initialAdminPassword = initialAdminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean emailProvided = initialAdminEmail != null && !initialAdminEmail.isBlank();
        boolean passwordProvided = initialAdminPassword != null && !initialAdminPassword.isBlank();

        if (!emailProvided && !passwordProvided) return;
        if (emailProvided != passwordProvided) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_EMAIL and INITIAL_ADMIN_PASSWORD must be configured together");
        }

        userManagementService.bootstrapInitialAdmin(initialAdminEmail, initialAdminPassword);
    }
}