package com.tsmc.lims.backend.lab.entity;

import com.tsmc.lims.backend.fabuser.entity.Experiment;
import com.tsmc.lims.backend.fabuser.entity.FabRequest;
import com.tsmc.lims.backend.lab.entity.enums.WipStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "wip_tasks")
@Getter @Setter @NoArgsConstructor
public class WipTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "request_id")
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", insertable = false, updatable = false)
    private FabRequest request;

    @Column(name = "wafer_code", nullable = false)
    private String waferCode;

    @Column(name = "exp_key")
    private String expKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exp_key", insertable = false, updatable = false)
    private Experiment experiment;

    @Column(name = "machine_id")
    private String machineId;

    @Column(name = "recipe_id")
    private String recipeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WipStatus status = WipStatus.QUEUE;

    @Column(name = "priority")
    private String priority = "NORMAL";

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
