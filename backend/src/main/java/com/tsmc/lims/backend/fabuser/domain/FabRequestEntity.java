package com.tsmc.lims.backend.fabuser.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "requests")
public class FabRequestEntity {

    @Id
    @Column(name = "request_id", length = 20)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private UserEntity requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private UserEntity approver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_id")
    private LaboratoryEntity laboratory;

    @Column(name = "priority", length = 20)
    private String priority = "NORMAL";

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "reject_reason")
    private String rejectReason;

    @ManyToMany
    @JoinTable(
            name = "request_experiments",
            joinColumns = @JoinColumn(name = "request_id"),
            inverseJoinColumns = @JoinColumn(name = "exp_key")
    )
    private List<ExperimentEntity> experiments = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected FabRequestEntity() {
    }

    public FabRequestEntity(String requestId, UserEntity requester, LaboratoryEntity laboratory, String priority, String remarks) {
        this.requestId = requestId;
        this.requester = requester;
        this.laboratory = laboratory;
        this.priority = priority;
        this.remarks = remarks;
        this.status = "PENDING";
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public String getRequestId() {
        return requestId;
    }

    public UserEntity getRequester() {
        return requester;
    }

    public UserEntity getApprover() {
        return approver;
    }

    public LaboratoryEntity getLaboratory() {
        return laboratory;
    }

    public String getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public List<ExperimentEntity> getExperiments() {
        return experiments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setExperiments(List<ExperimentEntity> experiments) {
        this.experiments = new ArrayList<>(experiments);
    }

    public void approve(UserEntity approver) {
        this.approver = approver;
        this.status = "APPROVED";
        this.rejectReason = null;
    }

    public void reject(UserEntity approver, String rejectReason) {
        this.approver = approver;
        this.status = "REJECTED";
        this.rejectReason = rejectReason;
    }
}
