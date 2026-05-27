package com.tsmc.lims.backend.fabuser.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "laboratories")
public class LaboratoryEntity {

    @Id
    @Column(name = "lab_id", length = 20)
    private String labId;

    @Column(name = "lab_name", nullable = false, length = 100)
    private String labName;

    protected LaboratoryEntity() {
    }

    public LaboratoryEntity(String labId, String labName) {
        this.labId = labId;
        this.labName = labName;
    }

    public String getLabId() {
        return labId;
    }

    public String getLabName() {
        return labName;
    }
}
