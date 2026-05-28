package com.tsmc.lims.backend.lab.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "machine_logs", indexes = {
    @Index(name = "idx_machine_logs_machine_id", columnList = "machine_id"),
    @Index(name = "idx_machine_logs_created_at", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor
public class MachineLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_id", nullable = false)
    private String machineId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** SYS / CFG / OPS / ALARM / MAINT */
    @Column(name = "level", nullable = false, length = 10)
    private String level;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    public MachineLog(String machineId, String level, String message) {
        this.machineId = machineId;
        this.level = level;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }
}
