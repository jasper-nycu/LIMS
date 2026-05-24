package com.tsmc.lims.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class DispatchRequest {

    @NotEmpty(message = "waferCodes must not be empty")
    private List<String> waferCodes;

    @NotBlank(message = "machineId must not be blank")
    private String machineId;

    @NotBlank(message = "recipeId must not be blank")
    private String recipeId;

    private String requestId = "REQ-SYS-INIT";
    private String expKey;
}
