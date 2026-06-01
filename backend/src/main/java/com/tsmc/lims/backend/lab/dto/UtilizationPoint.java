package com.tsmc.lims.backend.lab.dto;

public record UtilizationPoint(long timestamp, int utilization, String state) {}
