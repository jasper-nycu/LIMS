package com.tsmc.lims.backend.machine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "machines")
public class MachineEntity {

    @Id
    @Column(name = "machine_id")
    private String id;

    @Column(name = "lab_id")
    private String labId;

    @Column(name = "name")
    private String name;

    @Column(name = "exp_key")
    private String expKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private MachineState state;

    @Column(name = "capacity")
    private int cap;

    @Column(name = "loaded_count")
    private int loadedCount;

    @Column(name = "error_code")
    private String error;

    @Column(name = "current_utilization")
    private int currentUtil;

    @Transient
    private List<String> owners = new ArrayList<>();

    public MachineEntity() {
    }

    public MachineEntity(String id, String labId, String name, String expKey, MachineState state, int cap, int loadedCount, String error, int currentUtil, List<String> owners) {
        this.id = id;
        this.labId = labId;
        this.name = name;
        this.expKey = expKey;
        this.state = state;
        this.cap = cap;
        this.loadedCount = loadedCount;
        this.error = error;
        this.currentUtil = currentUtil;
        this.owners = owners;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabId() {
        return labId;
    }

    public void setLabId(String labId) {
        this.labId = labId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpKey() {
        return expKey;
    }

    public void setExpKey(String expKey) {
        this.expKey = expKey;
    }

    public MachineState getState() {
        return state;
    }

    public void setState(MachineState state) {
        this.state = state;
    }

    public int getCap() {
        return cap;
    }

    public void setCap(int cap) {
        this.cap = cap;
    }

    public int getLoadedCount() {
        return loadedCount;
    }

    public void setLoadedCount(int loadedCount) {
        this.loadedCount = loadedCount;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getCurrentUtil() {
        return currentUtil;
    }

    public void setCurrentUtil(int currentUtil) {
        this.currentUtil = currentUtil;
    }

    public List<String> getOwners() {
        return owners;
    }

    public void setOwners(List<String> owners) {
        this.owners = owners;
    }
}
