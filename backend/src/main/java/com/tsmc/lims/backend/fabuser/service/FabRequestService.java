package com.tsmc.lims.backend.fabuser.service;

import com.tsmc.lims.backend.auth.entity.User;
import com.tsmc.lims.backend.auth.repository.UserRepository;
import com.tsmc.lims.backend.auth.security.EcdsaCryptoProvider;
import com.tsmc.lims.backend.fabuser.dto.CreateFabRequest;
import com.tsmc.lims.backend.fabuser.dto.ExperimentOption;
import com.tsmc.lims.backend.fabuser.dto.FabRequestSummary;
import com.tsmc.lims.backend.fabuser.dto.LaboratoryOption;
import com.tsmc.lims.backend.fabuser.entity.Experiment;
import com.tsmc.lims.backend.fabuser.entity.FabRequest;
import com.tsmc.lims.backend.fabuser.entity.Laboratory;
import com.tsmc.lims.backend.fabuser.entity.Wafer;
import com.tsmc.lims.backend.fabuser.repository.ExperimentRepository;
import com.tsmc.lims.backend.fabuser.repository.FabRequestRepository;
import com.tsmc.lims.backend.fabuser.repository.LaboratoryRepository;
import com.tsmc.lims.backend.fabuser.repository.WaferRepository;
import com.tsmc.lims.backend.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.*;

@Service
public class FabRequestService {

    private static final Set<String> ALLOWED_PRIORITIES = Set.of("NORMAL", "URGENT", "CRITICAL");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final ExperimentRepository experimentRepository;
    private final FabRequestRepository requestRepository;
    private final WaferRepository waferRepository;
    private final NotificationService notificationService;
    private final EcdsaCryptoProvider cryptoProvider;

    public FabRequestService(
            UserRepository userRepository,
            LaboratoryRepository laboratoryRepository,
            ExperimentRepository experimentRepository,
            FabRequestRepository requestRepository,
            WaferRepository waferRepository,
            NotificationService notificationService,
            EcdsaCryptoProvider cryptoProvider
    ) {
        this.userRepository = userRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.experimentRepository = experimentRepository;
        this.requestRepository = requestRepository;
        this.waferRepository = waferRepository;
        this.notificationService = notificationService;
        this.cryptoProvider = cryptoProvider;
    }

    @Transactional(readOnly = true)
    public List<LaboratoryOption> listLaboratories() {
        return laboratoryRepository.findAll().stream()
                .map(lab -> {
                    List<ExperimentOption> exps = experimentRepository.findByLaboratoryLabIdOrderByExpKey(lab.getLabId()).stream()
                            .map(exp -> new ExperimentOption(exp.getExpKey(), exp.getExpName()))
                            .toList();
                    return new LaboratoryOption(lab.getLabId(), lab.getLabName(), exps);
                })
                .toList();
    }

    @Transactional
    public FabRequestSummary createRequest(CreateFabRequest incoming) {
        User requester = userRepository.findById(required(incoming.requesterId(), "requesterId"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requester user not found."));

        Laboratory laboratory = laboratoryRepository.findById(required(incoming.labId(), "labId"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target laboratory not found."));

        List<String> waferIds = normalizeWafers(incoming.waferIds());
        List<Experiment> experiments = normalizeExperiments(incoming.experimentKeys(), laboratory.getLabId());
        String priority = normalizePriority(incoming.priority());

        FabRequest request = new FabRequest(nextRequestId(), requester, laboratory, priority, incoming.remarks());
        request.setExperiments(experiments);

        // --- Cryptographic Non-Repudiation Digital Signature ---
        if (requester.getEncryptedPrivateKey() != null) {
            try {
                String payload = request.getRequestId() + ":" + requester.getEmployeeId() + ":" + String.join(",", waferIds);
                PrivateKey pk = cryptoProvider.decryptPrivateKey(requester.getEncryptedPrivateKey());
                String signature = cryptoProvider.signData(payload, pk);
                request.setRequesterSignature(payload, signature);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cryptographic signature generation failed.");
            }
        }

        FabRequest saved = requestRepository.save(request);
        waferIds.forEach(code -> waferRepository.save(new Wafer(saved, code)));

        // Broadcast notifications to supervisory layer via standardized audit service
        userRepository.findByRoleRoleEnumInAndIsActiveTrue(Set.of("ROLE_SYSADMIN", "ROLE_LAB_MANAGER")).stream()
                .filter(user -> !user.getEmployeeId().equals(requester.getEmployeeId()))
                .forEach(user -> notificationService.createNotification(
                        user.getEmployeeId(),
                        "New Fab Request",
                        saved.getRequestId() + " submitted by " + requester.getFirstName() + " " + requester.getLastName() + ".",
                        "info"
                ));

        notificationService.createNotification(
                requester.getEmployeeId(),
                "Request Submitted",
                saved.getRequestId() + " has been submitted for approval.",
                "success"
        );

        return toFabSummary(saved);
    }

    @Transactional(readOnly = true)
    public List<FabRequestSummary> listRequestsByRequester(String requesterId) {
        return requestRepository.findByRequesterEmployeeIdOrderByCreatedAtDesc(required(requesterId, "requesterId")).stream()
                .map(this::toFabSummary)
                .toList();
    }

    private FabRequestSummary toFabSummary(FabRequest request) {
        List<String> waferCodes = waferRepository.findByRequestRequestIdOrderByWaferCode(request.getRequestId()).stream()
                .map(Wafer::getWaferCode)
                .toList();
        List<String> expNames = request.getExperiments().stream()
                .map(Experiment::getExpName)
                .toList();

        return new FabRequestSummary(
                request.getRequestId(),
                request.getRequester() == null ? null : request.getRequester().getEmployeeId(),
                request.getLaboratory().getLabId(),
                waferCodes,
                expNames,
                waferCodes.size(),
                request.getStatus(),
                request.getPriority(),
                request.getRemarks(),
                request.getRejectReason(),
                request.getCreatedAt()
        );
    }

    private List<String> normalizeWafers(List<String> waferIds) {
        if (waferIds == null || waferIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one wafer ID is required.");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String wafer : waferIds) {
            String value = required(wafer, "waferId").toUpperCase(Locale.ROOT);
            if (!value.matches("^W-[0-9]{4}$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wafer ID must match W-XXXX.");
            }
            normalized.add(value);
        }
        if (normalized.size() != waferIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate wafer IDs are prohibited.");
        }
        return new ArrayList<>(normalized);
    }

    private List<Experiment> normalizeExperiments(List<String> experimentKeys, String labId) {
        if (experimentKeys == null || experimentKeys.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one experiment is required.");
        }
        Set<String> uniqueKeys = new LinkedHashSet<>(experimentKeys);
        List<Experiment> experiments = experimentRepository.findByExpKeyIn(uniqueKeys);
        if (experiments.size() != uniqueKeys.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more selected experiments do not exist.");
        }
        boolean labMismatch = experiments.stream()
                .anyMatch(exp -> !exp.getLaboratory().getLabId().equals(labId));
        if (labMismatch) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected experiments must belong to the target laboratory.");
        }
        return experiments.stream()
                .sorted(Comparator.comparing(Experiment::getExpKey))
                .toList();
    }

    private String normalizePriority(String priority) {
        String normalized = priority == null || priority.isBlank() ? "NORMAL" : priority.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PRIORITIES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Priority must be NORMAL, URGENT, or CRITICAL.");
        }
        return normalized;
    }

    private String nextRequestId() {
        String id;
        do {
            id = "REQ-" + (100000 + RANDOM.nextInt(900000));
        } while (requestRepository.existsById(id));
        return id;
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required.");
        }
        return value.trim();
    }
}