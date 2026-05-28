package com.tsmc.lims.backend.lab.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String machineId, String from, String to) {
        super("Machine " + machineId + " cannot transition from " + from + " to " + to);
    }
}
