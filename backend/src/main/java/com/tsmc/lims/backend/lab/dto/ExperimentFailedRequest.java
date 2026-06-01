package com.tsmc.lims.backend.lab.dto;

import java.util.List;

public record ExperimentFailedRequest(List<String> waferCodes) {}
