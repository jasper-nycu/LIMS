package com.tsmc.lims.backend.repository;

import com.tsmc.lims.backend.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, String> {
    List<Recipe> findByMachineMachineId(String machineId);
    Optional<Recipe> findByMachineMachineIdAndName(String machineId, String name);
}
