package com.tsmc.lims.backend.lab.repository;

import com.tsmc.lims.backend.lab.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    List<User> findByRoleRoleEnumInAndIsActiveTrue(Collection<String> roleEnums);
}
