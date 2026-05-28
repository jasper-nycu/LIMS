package com.tsmc.lims.backend.lab.entity;

import com.tsmc.lims.backend.lab.entity.enums.MachineState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "machines")
@Getter @Setter @NoArgsConstructor
public class Machine {

    @Id
    @Column(name = "machine_id")
    private String machineId;

    @Column(name = "lab_id")
    private String labId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "exp_key")
    private String expKey;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private MachineState state = MachineState.IDLE;

    @Column(name = "current_utilization")
    private int currentUtilization = 0;

    @Column(name = "error_code")
    private String errorCode;

    // Not persisted — populated at query time from WipTask count
    @Transient
    private int loadedCount = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "machine_owners", joinColumns = @JoinColumn(name = "machine_id"))
    @Column(name = "owner_initials")
    private List<String> owners = new ArrayList<>();
}
