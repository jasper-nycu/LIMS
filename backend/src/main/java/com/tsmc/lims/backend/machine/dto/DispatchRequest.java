package com.tsmc.lims.backend.machine.dto;

import java.util.List;

public class DispatchRequest {
    private List<String> waferIds;

    public List<String> getWaferIds() {
        return waferIds;
    }

    public void setWaferIds(List<String> waferIds) {
        this.waferIds = waferIds;
    }
}
