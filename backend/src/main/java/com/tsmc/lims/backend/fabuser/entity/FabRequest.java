package com.tsmc.lims.backend.fabuser.entity;

import com.tsmc.lims.backend.auth.entity.User;

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
public class FabRequest {

    @Id
    @Column(name = "request_id", length = 20)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_id")
    private Laboratory laboratory;

    @Column(name = "priority", length = 20)
    private String priority = "NORMAL";

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "signature_payload_hash")
    private String signaturePayloadHash;

    @Column(name = "requester_signature", columnDefinition = "TEXT")
    private String requesterSignature;

    @Column(name = "approver_signature", columnDefinition = "TEXT")
    private String approverSignature;

    @ManyToMany
    @JoinTable(
            name = "request_experiments",
            joinColumns = @JoinColumn(name = "request_id"),
            inverseJoinColumns = @JoinColumn(name = "exp_key")
    )
    private List<Experiment> experiments = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected FabRequest() {
    }

    public FabRequest(String requestId, User requester, Laboratory laboratory, String priority, String remarks) {
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

    public User getRequester() {
        return requester;
    }

    public User getApprover() {
        return approver;
    }

    public Laboratory getLaboratory() {
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

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setExperiments(List<Experiment> experiments) {
        this.experiments = new ArrayList<>(experiments);
    }

    public void setRequesterSignature(String payloadHash, String signature) {
        this.signaturePayloadHash = payloadHash;
        this.requesterSignature = signature;
    }

    public void approve(User approver, String approverSignature) {
        this.approver = approver;
        this.status = "APPROVED";
        this.rejectReason = null;
        this.approverSignature = approverSignature;
    }

    public void reject(User approver, String rejectReason, String approverSignature) {
        this.approver = approver;
        this.status = "REJECTED";
        this.rejectReason = rejectReason;
        this.approverSignature = approverSignature;
    }
}