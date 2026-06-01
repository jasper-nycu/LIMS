package com.tsmc.lims.backend.lab.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recipes")
@Getter @Setter @NoArgsConstructor
public class Recipe {

    @Id
    @Column(name = "recipe_id")
    private String id;

    @Column(name = "recipe_name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    public Recipe(String id, String name, Machine machine) {
        this.id = id;
        this.name = name;
        this.machine = machine;
    }
}
