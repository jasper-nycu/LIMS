package com.tsmc.lims.backend.machine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipes")
public class RecipeEntity {

    @Id
    @Column(name = "recipe_id")
    private String id;

    @Column(name = "recipe_name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private MachineEntity machine;

    public RecipeEntity() {
    }

    public RecipeEntity(String id, String name, MachineEntity machine) {
        this.id = id;
        this.name = name;
        this.machine = machine;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MachineEntity getMachine() {
        return machine;
    }

    public void setMachine(MachineEntity machine) {
        this.machine = machine;
    }
}
