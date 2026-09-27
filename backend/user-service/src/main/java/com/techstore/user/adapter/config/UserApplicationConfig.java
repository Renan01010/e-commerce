package com.techstore.user.adapter.config;

import com.techstore.user.application.port.out.PasswordHasher;
import com.techstore.user.application.port.out.TransactionRunner;
import com.techstore.user.application.port.out.UserRepository;
import com.techstore.user.application.port.out.AccessTokenIssuer;
import com.techstore.user.application.service.AuthService;
import com.techstore.user.application.service.UserManagementService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserApplicationConfig {
    @Bean
    Clock userServiceClock() {
        return Clock.systemUTC();
    }

    @Bean
    UserManagementService userManagementService(UserRepository users, PasswordHasher passwordHasher,
                                               TransactionRunner transactions, Clock userServiceClock) {
        return new UserManagementService(users, passwordHasher, transactions, userServiceClock);
    }

    @Bean
    AuthService authService(UserRepository users, PasswordHasher passwordHasher,
                            AccessTokenIssuer tokenIssuer, Clock userServiceClock) {
        return new AuthService(users, passwordHasher, tokenIssuer, userServiceClock);
    }
}