package com.tsmc.lims.backend.repository;

import com.tsmc.lims.backend.domain.LaboratoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaboratoryRepository extends JpaRepository<LaboratoryEntity, String> {
}
