package com.tsmc.lims.backend.fabuser.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tsmc.lims.backend.fabuser.entity.Laboratory;

public interface LaboratoryRepository extends JpaRepository<Laboratory, String> {
}
