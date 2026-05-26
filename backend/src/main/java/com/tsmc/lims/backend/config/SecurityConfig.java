package com.tsmc.lims.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List; // List.of vs Arrays.asList - List.of is immutable and more concise for fixed-size lists, while Arrays.asList allows modifications but can lead to issues if not used carefully. In this case, List.of is preferred for its simplicity and immutability.

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Read allowed origins from application.properties with a default value of http://localhost:5173 (Vite dev server)
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    /**
     * Exposes the BCryptPasswordEncoder as a Spring Bean for use in AuthService.
     * BCrypt Hashing format: $2a$10$<salt><hash> - The salt is automatically generated and stored within the hash string.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures route protection.
     * We disable Cross-Site Request Forgery (CSRF) (as we use Stateless JWTs) and whitelist the Auth endpoints.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF since we are using stateless JWTs for authentication
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll() // DMZ: Allow unauthenticated access to authentication endpoints
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated() // All other endpoints require authentication (protected by JWT)
            );
        return http.build();
    }

    /**
     * Configures CORS to allow requests from the frontend Vite server (http://localhost:5173).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins); // like Firewall rules, we only allow requests from our trusted frontend port (Vite dev server)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}