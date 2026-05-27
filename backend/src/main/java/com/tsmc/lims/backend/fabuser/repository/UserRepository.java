package com.tsmc.lims.backend.fabuser.repository;

import com.tsmc.lims.backend.fabuser.domain.UserEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    List<UserEntity> findByRoleEnumInAndActiveTrue(Collection<String> roleEnums);
}
