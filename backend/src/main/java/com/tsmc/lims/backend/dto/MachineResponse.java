package com.tsmc.lims.backend.dto;

import com.tsmc.lims.backend.entity.enums.MachineState;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MachineResponse {
    private String id;
    private String name;
    private String expKey;
    private MachineState state;
    private int cap;
    private int loadedCount;
    private List<String> loadedWafers;
    private String error;
    private int currentUtil;
    private List<String> owners;
}
