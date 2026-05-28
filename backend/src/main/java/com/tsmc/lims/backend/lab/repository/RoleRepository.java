package com.tsmc.lims.backend.lab.repository;

import com.tsmc.lims.backend.lab.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
}
