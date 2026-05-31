package com.tsmc.lims.backend.labmanager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.tsmc.lims.backend.fabuser.entity.Experiment;
import com.tsmc.lims.backend.fabuser.entity.FabRequest;

@Entity
@Table(name = "wip_tasks")
public class WipTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Integer taskId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private FabRequest request;

    @Column(name = "wafer_code", nullable = false, length = 50)
    private String waferCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exp_key")
    private Experiment experiment;

    @Column(name = "status", length = 20)
    private String status = "QUEUE";

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    protected WipTask() {
    }

    public WipTask(FabRequest request, String waferCode, Experiment experiment) {
        this.request = request;
        this.waferCode = waferCode;
        this.experiment = experiment;
        this.status = "QUEUE";
    }

    @PrePersist
    void prePersist() {
        if (dispatchedAt == null) {
            dispatchedAt = LocalDateTime.now();
        }
    }

    // --- Getters and Setters ---

    public Integer getTaskId() {
        return taskId;
    }

    public FabRequest getRequest() {
        return request;
    }

    public String getWaferCode() {
        return waferCode;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }
}
