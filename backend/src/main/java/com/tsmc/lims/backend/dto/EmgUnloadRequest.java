package com.tsmc.lims.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmgUnloadRequest {

    public enum Action { SCRAP, REUSE }

    @NotNull(message = "action must not be null")
    private Action action;
}
