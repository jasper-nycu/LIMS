package com.tsmc.lims.backend.fabuser.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "wafers", uniqueConstraints = @UniqueConstraint(columnNames = {"request_id", "wafer_code"}))
public class WaferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wafer_internal_id")
    private Integer waferInternalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private FabRequestEntity request;

    @Column(name = "wafer_code", nullable = false, length = 50)
    private String waferCode;

    protected WaferEntity() {
    }

    public WaferEntity(FabRequestEntity request, String waferCode) {
        this.request = request;
        this.waferCode = waferCode;
    }

    public Integer getWaferInternalId() {
        return waferInternalId;
    }

    public FabRequestEntity getRequest() {
        return request;
    }

    public String getWaferCode() {
        return waferCode;
    }
}
