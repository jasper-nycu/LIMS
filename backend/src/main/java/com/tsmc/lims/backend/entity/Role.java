package com.tsmc.lims.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @Column(name = "role_enum", length = 50, nullable = false)
    private String roleEnum;

    @Column(name = "role_name", length = 100, nullable = false)
    private String roleName;

    @Column(name = "permissions", columnDefinition = "jsonb")
    private String permissions;
}
