package com.tsmc.lims.backend.labmanager.service;

import com.tsmc.lims.backend.auth.entity.User;
import com.tsmc.lims.backend.auth.repository.UserRepository;
import com.tsmc.lims.backend.auth.security.EcdsaCryptoProvider;
import com.tsmc.lims.backend.fabuser.entity.FabRequest;
import com.tsmc.lims.backend.fabuser.entity.Wafer;
import com.tsmc.lims.backend.fabuser.repository.FabRequestRepository;
import com.tsmc.lims.backend.fabuser.repository.WaferRepository;
import com.tsmc.lims.backend.labmanager.dto.LabWipSummary;
import com.tsmc.lims.backend.labmanager.dto.ManagerRequestSummary;
import com.tsmc.lims.backend.lab.entity.WipTask;
import com.tsmc.lims.backend.lab.entity.enums.WipStatus;
import com.tsmc.lims.backend.lab.repository.WipTaskRepository;
import com.tsmc.lims.backend.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.PrivateKey;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class LabManagerService {

    private static final DateTimeFormatter MANAGER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FabRequestRepository requestRepository;
    private final WipTaskRepository wipTaskRepository;
    private final UserRepository userRepository;
    private final WaferRepository waferRepository;
    private final NotificationService notificationService;
    private final EcdsaCryptoProvider cryptoProvider;

    public LabManagerService(
            FabRequestRepository requestRepository,
            WipTaskRepository wipTaskRepository,
            UserRepository userRepository,
            WaferRepository waferRepository,
            NotificationService notificationService,
            EcdsaCryptoProvider cryptoProvider
    ) {
        this.requestRepository = requestRepository;
        this.wipTaskRepository = wipTaskRepository;
        this.userRepository = userRepository;
        this.waferRepository = waferRepository;
        this.notificationService = notificationService;
        this.cryptoProvider = cryptoProvider;
    }

    @Transactional(readOnly = true)
    public List<ManagerRequestSummary> listPendingRequests() {
        return requestRepository.findByStatusOrderedForManagerQueue("PENDING").stream()
                .map(this::toManagerSummary)
                .toList();
    }

    @Transactional
    public ManagerRequestSummary approveRequest(String requestId, String approverId) {
        FabRequest request = loadRequest(requestId);
        ensurePending(request);

        User approver = userRepository.findById(required(approverId, "approverId"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approver supervisor not found."));

        // --- Cryptographic Non-Repudiation Signature for Approval ---
        String approverSignature = null;
        if (approver.getEncryptedPrivateKey() != null) {
            try {
                String payload = "APPROVE:" + request.getRequestId() + ":" + approver.getEmployeeId();
                PrivateKey pk = cryptoProvider.decryptPrivateKey(approver.getEncryptedPrivateKey());
                approverSignature = cryptoProvider.signData(payload, pk);
            } catch (Exception e) {
                // Key decryption failed (e.g. AES master key rotation) — skip signature, approval still proceeds
            }
        }

        request.approve(approver, approverSignature);
        FabRequest saved = requestRepository.save(request);

        // Dispatch items down to production line WIP tasks queue
        List<Wafer> wafers = waferRepository.findByRequestRequestIdOrderByWaferCode(saved.getRequestId());
        saved.getExperiments().forEach(exp -> wafers.forEach(wafer -> {
            WipTask task = new WipTask();
            task.setRequestId(saved.getRequestId());
            task.setWaferCode(wafer.getWaferCode());
            task.setExpKey(exp.getExpKey());
            wipTaskRepository.save(task);
        }));

        User requester = saved.getRequester();
        if (requester != null) {
            notificationService.createNotification(
                    requester.getEmployeeId(),
                    "Request Approved",
                    saved.getRequestId() + " has been approved by " + approver.getFirstName() + " " + approver.getLastName() + ".",
                    "success"
            );
        }

        // Notify the approver manager for audit and confirmation
        notificationService.createNotification(
                approver.getEmployeeId(),
                "Request Approved",
                "You have successfully approved request " + saved.getRequestId() + ".",
                "success"
        );

        return toManagerSummary(saved);
    }

    @Transactional
    public ManagerRequestSummary rejectRequest(String requestId, String approverId, String rejectReason) {
        FabRequest request = loadRequest(requestId);
        ensurePending(request);

        if (rejectReason == null || rejectReason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reject reason is required.");
        }

        User approver = userRepository.findById(required(approverId, "approverId"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approver supervisor not found."));

        // --- Cryptographic Non-Repudiation Signature for Rejection ---
        String approverSignature = null;
        if (approver.getEncryptedPrivateKey() != null) {
            try {
                String payload = "REJECT:" + request.getRequestId() + ":" + approver.getEmployeeId();
                PrivateKey pk = cryptoProvider.decryptPrivateKey(approver.getEncryptedPrivateKey());
                approverSignature = cryptoProvider.signData(payload, pk);
            } catch (Exception e) {
                // Key decryption failed — skip signature, rejection still proceeds
            }
        }

        request.reject(approver, rejectReason.trim(), approverSignature);
        FabRequest saved = requestRepository.save(request);

        User requester = saved.getRequester();
        if (requester != null) {
            notificationService.createNotification(
                    requester.getEmployeeId(),
                    "Request Rejected",
                    saved.getRequestId() + " has been rejected by " + approver.getFirstName() + " " + approver.getLastName() + ". Reason: " + rejectReason.trim(),
                    "error"
            );
        }

        // Notify the manager themselves for action confirmation
        notificationService.createNotification(
                approver.getEmployeeId(),
                "Request Rejected",
                "You have rejected request " + saved.getRequestId() + ".",
                "warning"
        );

        return toManagerSummary(saved);
    }

    @Transactional(readOnly = true)
    public List<LabWipSummary> listPendingWips() {
        return wipTaskRepository.findByStatusInOrderedForSorting(List.of(WipStatus.QUEUE, WipStatus.PENDING_SORTING)).stream()
                .map(task -> new LabWipSummary(
                        task.getWaferCode() + "-" + task.getExperiment().getExpKey() + "-" + task.getTaskId(),
                        task.getWaferCode(),
                        task.getExperiment().getExpKey(),
                        task.getRequest().getPriority()
                ))
                .toList();
    }

    private FabRequest loadRequest(String requestId) {
        return requestRepository.findById(requestId != null ? requestId.trim() : "")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request item not found."));
    }

    private void ensurePending(FabRequest request) {
        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING requests can be processed.");
        }
    }

    private ManagerRequestSummary toManagerSummary(FabRequest request) {
        User requester = request.getRequester();
        return new ManagerRequestSummary(
                request.getRequestId(),
                requester == null ? "System" : requester.getFirstName() + " " + requester.getLastName(),
                requester == null ? "System" : requester.getRole().getRoleEnum().replace("ROLE_", "").toUpperCase(Locale.ROOT),
                waferRepository.findByRequestRequestIdOrderByWaferCode(request.getRequestId()).stream()
                        .map(Wafer::getWaferCode)
                        .toList(),
                request.getExperiments().stream()
                        .map(exp -> exp.getExpKey())
                        .toList(),
                request.getRemarks() == null ? "" : request.getRemarks(),
                request.getPriority(),
                request.getCreatedAt() == null ? "" : request.getCreatedAt().format(MANAGER_TIME_FORMAT)
        );
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required.");
        }
        return value.trim();
    }
}