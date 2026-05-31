package com.tsmc.lims.backend.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity // Tell Spring Boot this class maps to a database table
@Table(name = "users") // Specify that this maps to the "users" table
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id // Mark this field as the Primary Key
    @Column(name = "employee_id", length = 20, nullable = false) // Map to employee_id column
    private String employeeId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_enum", referencedColumnName = "role_enum")
    private Role role;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "title", length = 10)
    private String title;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "email", length = 100, unique = true, nullable = false)
    private String email;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "extension", length = 10)
    private String extension;

    @Column(name = "mobile_phone", length = 20)
    private String mobilePhone;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    // --- Security & Cryptography Fields ---

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "password_salt", nullable = false)
    private String passwordSalt;

    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "encrypted_private_key", columnDefinition = "TEXT")
    private String encryptedPrivateKey;

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "password_modified_at")
    private LocalDateTime passwordModifiedAt = LocalDateTime.now();
}