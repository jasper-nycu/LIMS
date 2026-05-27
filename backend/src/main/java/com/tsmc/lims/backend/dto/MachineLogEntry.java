package com.tsmc.lims.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MachineLogEntry {
    private LocalDateTime timestamp;
    private String level;
    private String message;

    public String toLine() {
        return String.format("[%s] %s: %s", timestamp.toString().replace("T", " ").substring(0, 19), level, message);
    }
}
