package com.tsmc.lims.backend.auth.repository;

import com.tsmc.lims.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    // Find user by email for registration and TOTP verification
    Optional<User> findByEmail(String email);

    // Check if email already exists during registration
    boolean existsByEmail(String email);

    // Method for broadcasting notifications to Fab and Lab
    List<User> findByRoleRoleEnumInAndIsActiveTrue(Collection<String> roleEnums);
}