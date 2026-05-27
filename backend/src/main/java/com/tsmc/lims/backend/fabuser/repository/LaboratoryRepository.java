package com.tsmc.lims.backend.fabuser.repository;

import com.tsmc.lims.backend.fabuser.domain.LaboratoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaboratoryRepository extends JpaRepository<LaboratoryEntity, String> {
}
