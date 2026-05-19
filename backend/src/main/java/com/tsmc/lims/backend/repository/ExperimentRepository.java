package com.tsmc.lims.backend.repository;

import com.tsmc.lims.backend.domain.ExperimentEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperimentRepository extends JpaRepository<ExperimentEntity, String> {
    List<ExperimentEntity> findByLaboratoryLabIdOrderByExpKey(String labId);

    List<ExperimentEntity> findByExpKeyIn(Collection<String> expKeys);
}
