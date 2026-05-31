package com.tsmc.lims.backend.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity // Tell Spring Boot this class maps to a database table
@Table(name = "roles") // Specify that this maps to the "roles" table
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id // Mark this field as the Primary Key
    @Column(name = "role_enum", length = 50, nullable = false) // Map to role_enum column
    private String roleEnum;

    @Column(name = "role_name", length = 100, nullable = false)
    private String roleName;

    @Column(name = "permissions", columnDefinition = "jsonb")
    private String permissions;
}