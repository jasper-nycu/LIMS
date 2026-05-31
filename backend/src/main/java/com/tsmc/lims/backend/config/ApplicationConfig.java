package com.tsmc.lims.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ApplicationConfig {

    /**
     * Exposes the BCryptPasswordEncoder as a Spring Bean for use in AuthService.
     * BCrypt Hashing format: $2a$10$<salt><hash> - The salt is automatically generated and stored within the hash string.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}