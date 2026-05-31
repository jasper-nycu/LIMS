package com.tsmc.lims.backend.auth.security;

import com.tsmc.lims.backend.auth.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long expirationHours;

    // Inject properties from application.properties
    public JwtProvider(
            @Value("${lims.security.jwt.secret}") String secret,
            @Value("${lims.security.jwt.expiration-hours:24}") long expirationHours) {
        // Generate a cryptographic key from the secret string
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    /**
     * Generates a cryptographically signed JSON Web Token (JWT).
     */
    public String generateToken(User user) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + (expirationHours * 60 * 60 * 1000);

        return Jwts.builder()
                .subject(user.getEmployeeId())
                .claim("name", user.getFirstName() + " " + user.getLastName())
                .claim("role", user.getRole().getRoleEnum())
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(expMillis))
                .signWith(key)
                .compact();
    }
}