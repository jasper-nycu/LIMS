package com.tsmc.lims.backend.repository;

import com.tsmc.lims.backend.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, String> {
    List<Recipe> findByMachineMachineId(String machineId);
}
