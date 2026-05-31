package com.tsmc.lims.backend.fabuser.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tsmc.lims.backend.fabuser.entity.Experiment;

public interface ExperimentRepository extends JpaRepository<Experiment, String> {
    List<Experiment> findByLaboratoryLabIdOrderByExpKey(String labId);

    List<Experiment> findByExpKeyIn(Collection<String> expKeys);
}
