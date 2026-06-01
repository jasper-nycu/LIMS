package com.tsmc.lims.backend.lab.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmgUnloadRequest {

    public enum Action { SCRAP, REUSE }

    @NotNull(message = "action must not be null")
    private Action action;
}
