package com.tsmc.lims.backend.entity;

import com.tsmc.lims.backend.entity.enums.MachineState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

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

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private MachineState state = MachineState.IDLE;

    @Column(name = "current_utilization")
    private int currentUtilization = 0;

    @Column(name = "error_code")
    private String errorCode;
}
