package com.tsmc.lims.backend.machine.dto;

import com.tsmc.lims.backend.machine.MachineState;
import java.util.List;

public class MachineResponse {
    private String id;
    private String name;
    private String expKey;
    private MachineState state;
    private int cap;
    private int loadedCount;
    private String error;
    private int currentUtil;
    private List<String> owners;

    public MachineResponse() {
    }

    public MachineResponse(String id, String name, String expKey, MachineState state, int cap, int loadedCount, String error, int currentUtil, List<String> owners) {
        this.id = id;
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
