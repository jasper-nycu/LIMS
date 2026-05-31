package com.tsmc.lims.backend.fabuser.controller;

import com.tsmc.lims.backend.fabuser.dto.CreateFabRequest;
import com.tsmc.lims.backend.fabuser.dto.FabRequestSummary;
import com.tsmc.lims.backend.fabuser.dto.LaboratoryOption;
import com.tsmc.lims.backend.fabuser.service.FabRequestService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fab")
@PreAuthorize("hasAnyRole('SYSADMIN', 'FAB_USER')")
public class FabRequestController {

    private final FabRequestService service;

    public FabRequestController(FabRequestService service) {
        this.service = service;
    }

    @GetMapping("/labs")
    public List<LaboratoryOption> listLabs() {
        return service.listLaboratories();
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public FabRequestSummary createRequest(@RequestBody CreateFabRequest request) {
        return service.createRequest(request);
    }

    @GetMapping("/requests")
    public List<FabRequestSummary> listRequests(@RequestParam String requesterId) {
        return service.listRequestsByRequester(requesterId);
    }
}
