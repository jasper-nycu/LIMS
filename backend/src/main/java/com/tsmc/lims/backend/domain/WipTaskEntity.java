package com.tsmc.lims.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "wip_tasks")
public class WipTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private FabRequestEntity request;

    @Column(name = "wafer_code", nullable = false, length = 50)
    private String waferCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exp_key")
    private ExperimentEntity experiment;

    @Column(name = "status", length = 20)
    private String status = "QUEUE";

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    protected WipTaskEntity() {
    }

    public WipTaskEntity(FabRequestEntity request, String waferCode, ExperimentEntity experiment) {
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

    public Long getTaskId() {
        return taskId;
    }

    public FabRequestEntity getRequest() {
        return request;
    }

    public String getWaferCode() {
        return waferCode;
    }

    public ExperimentEntity getExperiment() {
        return experiment;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }
}
