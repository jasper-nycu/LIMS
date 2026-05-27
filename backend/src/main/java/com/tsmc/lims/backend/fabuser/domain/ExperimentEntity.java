package com.tsmc.lims.backend.fabuser.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "experiments")
public class ExperimentEntity {

    @Id
    @Column(name = "exp_key", length = 50)
    private String expKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_id")
    private LaboratoryEntity laboratory;

    @Column(name = "exp_name", nullable = false, length = 100)
    private String expName;

    protected ExperimentEntity() {
    }

    public ExperimentEntity(String expKey, LaboratoryEntity laboratory, String expName) {
        this.expKey = expKey;
        this.laboratory = laboratory;
        this.expName = expName;
    }

    public String getExpKey() {
        return expKey;
    }

    public LaboratoryEntity getLaboratory() {
        return laboratory;
    }

    public String getExpName() {
        return expName;
    }
}
