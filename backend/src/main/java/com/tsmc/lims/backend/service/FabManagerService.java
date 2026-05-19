package com.tsmc.lims.backend.service;

import com.tsmc.lims.backend.domain.ExperimentEntity;
import com.tsmc.lims.backend.domain.FabRequestEntity;
import com.tsmc.lims.backend.domain.LaboratoryEntity;
import com.tsmc.lims.backend.domain.NotificationEntity;
import com.tsmc.lims.backend.domain.UserEntity;
import com.tsmc.lims.backend.domain.WaferEntity;
import com.tsmc.lims.backend.domain.WipTaskEntity;
import com.tsmc.lims.backend.dto.CreateFabRequest;
import com.tsmc.lims.backend.dto.ExperimentOption;
import com.tsmc.lims.backend.dto.FabRequestSummary;
import com.tsmc.lims.backend.dto.LaboratoryOption;
import com.tsmc.lims.backend.dto.LabWipSummary;
import com.tsmc.lims.backend.dto.ManagerRequestSummary;
import com.tsmc.lims.backend.dto.NotificationSummary;
import com.tsmc.lims.backend.repository.ExperimentRepository;
import com.tsmc.lims.backend.repository.FabRequestRepository;
import com.tsmc.lims.backend.repository.LaboratoryRepository;
import com.tsmc.lims.backend.repository.NotificationRepository;
import com.tsmc.lims.backend.repository.UserRepository;
import com.tsmc.lims.backend.repository.WaferRepository;
import com.tsmc.lims.backend.repository.WipTaskRepository;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FabManagerService {

    private static final DateTimeFormatter MANAGER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("NORMAL", "URGENT", "CRITICAL");

    private final UserRepository userRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final ExperimentRepository experimentRepository;
    private final FabRequestRepository requestRepository;
    private final WaferRepository waferRepository;
    private final WipTaskRepository wipTaskRepository;
    private final NotificationRepository notificationRepository;

    public FabManagerService(
            UserRepository userRepository,
            LaboratoryRepository laboratoryRepository,
            ExperimentRepository experimentRepository,
            FabRequestRepository requestRepository,
            WaferRepository waferRepository,
            WipTaskRepository wipTaskRepository,
            NotificationRepository notificationRepository
    ) {
        this.userRepository = userRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.experimentRepository = experimentRepository;
        this.requestRepository = requestRepository;
        this.waferRepository = waferRepository;
        this.wipTaskRepository = wipTaskRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<LaboratoryOption> listLaboratories() {
        return laboratoryRepository.findAll().stream()
                .sorted(Comparator.comparing(LaboratoryEntity::getLabId))
                .map(lab -> new LaboratoryOption(
                        lab.getLabId(),
                        lab.getLabName(),
                        experimentRepository.findByLaboratoryLabIdOrderByExpKey(lab.getLabId()).stream()
                                .map(exp -> new ExperimentOption(exp.getExpKey(), exp.getExpName()))
                                .toList()
                ))
                .toList();
    }

    @Transactional
    public FabRequestSummary createRequest(CreateFabRequest incoming) {
        if (incoming == null) {
            throw badRequest("Request body is required.");
        }

        UserEntity requester = userRepository.findById(required(incoming.requesterId(), "requesterId"))
                .orElseThrow(() -> notFound("Requester not found."));
        LaboratoryEntity laboratory = laboratoryRepository.findById(required(incoming.labId(), "labId"))
                .orElseThrow(() -> notFound("Laboratory not found."));

        List<String> waferIds = normalizeWafers(incoming.waferIds());
        List<ExperimentEntity> experiments = normalizeExperiments(incoming.experimentKeys(), laboratory.getLabId());
        String priority = normalizePriority(incoming.priority());

        FabRequestEntity request = new FabRequestEntity(nextRequestId(), requester, laboratory, priority, incoming.remarks());
        request.setExperiments(experiments);
        FabRequestEntity saved = requestRepository.save(request);

        waferIds.stream()
                .map(wafer -> new WaferEntity(saved, wafer))
                .forEach(waferRepository::save);

        notifyRolesExcept(
                Set.of("ROLE_SYSADMIN", "ROLE_LAB_MANAGER"),
                requester.getEmployeeId(),
                "New Fab Request",
                saved.getRequestId() + " submitted by " + requester.getDisplayName() + ".",
                "info"
        );
        notificationRepository.save(new NotificationEntity(
                requester,
                "Request Submitted",
                saved.getRequestId() + " has been submitted for approval.",
                "success"
        ));

        return toFabSummary(saved);
    }

    @Transactional(readOnly = true)
    public List<FabRequestSummary> listRequesterRequests(String requesterId) {
        return requestRepository.findByRequesterEmployeeIdOrderByCreatedAtDesc(required(requesterId, "requesterId")).stream()
                .map(this::toFabSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ManagerRequestSummary> listPendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtAsc("PENDING").stream()
                .map(this::toManagerSummary)
                .toList();
    }

    @Transactional
    public ManagerRequestSummary approveRequest(String requestId, String approverId) {
        FabRequestEntity request = loadRequest(requestId);
        ensurePending(request);
        UserEntity approver = userRepository.findById(defaultIfBlank(approverId, "TS-9001"))
                .orElseThrow(() -> notFound("Approver not found."));

        request.approve(approver);

        if (!wipTaskRepository.existsByRequestRequestId(request.getRequestId())) {
            List<WaferEntity> wafers = waferRepository.findByRequestRequestIdOrderByWaferCode(request.getRequestId());
            for (WaferEntity wafer : wafers) {
                for (ExperimentEntity experiment : request.getExperiments()) {
                    wipTaskRepository.save(new WipTaskEntity(request, wafer.getWaferCode(), experiment));
                }
            }
        }

        notifyRoles(
                Set.of("ROLE_SYSADMIN", "ROLE_FAB_USER", "ROLE_LAB_MANAGER"),
                "Request Approved",
                request.getRequestId() + " approved and moved to Lab WIP queue.",
                "success"
        );

        return toManagerSummary(request);
    }

    @Transactional
    public ManagerRequestSummary rejectRequest(String requestId, String approverId, String rejectReason) {
        FabRequestEntity request = loadRequest(requestId);
        ensurePending(request);
        if (rejectReason == null || rejectReason.isBlank()) {
            throw badRequest("rejectReason is required.");
        }
        UserEntity approver = userRepository.findById(defaultIfBlank(approverId, "TS-9001"))
                .orElseThrow(() -> notFound("Approver not found."));
        request.reject(approver, rejectReason.trim());
        notifyRoles(
                Set.of("ROLE_SYSADMIN", "ROLE_FAB_USER", "ROLE_LAB_MANAGER"),
                "Request Rejected",
                request.getRequestId() + " rejected: " + rejectReason.trim(),
                "error"
        );
        return toManagerSummary(request);
    }

    @Transactional(readOnly = true)
    public List<NotificationSummary> listNotifications(String employeeId) {
        return notificationRepository.findByUserEmployeeIdOrderByCreatedAtDesc(required(employeeId, "employeeId")).stream()
                .map(notification -> new NotificationSummary(
                        String.valueOf(notification.getNotifId()),
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.getType(),
                        notification.isRead(),
                        notification.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LabWipSummary> listPendingWips() {
        return wipTaskRepository.findByStatusOrderByDispatchedAtAsc("QUEUE").stream()
                .map(task -> new LabWipSummary(
                        task.getWaferCode() + "-" + task.getExperiment().getExpKey() + "-" + task.getTaskId(),
                        task.getExperiment().getExpKey(),
                        task.getRequest().getPriority()
                ))
                .toList();
    }

    @Transactional
    public void markNotificationsRead(String employeeId) {
        notificationRepository.findByUserEmployeeIdOrderByCreatedAtDesc(required(employeeId, "employeeId"))
                .forEach(NotificationEntity::markRead);
    }

    @Transactional
    public void deleteNotification(String employeeId, Long notificationId) {
        required(employeeId, "employeeId");
        if (notificationId == null) {
            throw badRequest("notificationId is required.");
        }
        notificationRepository.deleteByNotifIdAndUserEmployeeId(notificationId, employeeId);
    }

    @Transactional
    public void clearNotifications(String employeeId) {
        notificationRepository.deleteByUserEmployeeId(required(employeeId, "employeeId"));
    }

    private FabRequestEntity loadRequest(String requestId) {
        return requestRepository.findById(required(requestId, "requestId"))
                .orElseThrow(() -> notFound("Request not found."));
    }

    private void ensurePending(FabRequestEntity request) {
        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING requests can be decided.");
        }
    }

    private FabRequestSummary toFabSummary(FabRequestEntity request) {
        List<String> wafers = waferRepository.findByRequestRequestIdOrderByWaferCode(request.getRequestId()).stream()
                .map(WaferEntity::getWaferCode)
                .toList();
        List<String> experiments = request.getExperiments().stream()
                .map(ExperimentEntity::getExpName)
                .toList();

        return new FabRequestSummary(
                request.getRequestId(),
                request.getRequester() == null ? null : request.getRequester().getEmployeeId(),
                request.getLaboratory().getLabId(),
                wafers,
                experiments,
                wafers.size(),
                request.getStatus(),
                request.getPriority(),
                request.getRemarks(),
                request.getRejectReason(),
                request.getCreatedAt()
        );
    }

    private ManagerRequestSummary toManagerSummary(FabRequestEntity request) {
        UserEntity requester = request.getRequester();
        return new ManagerRequestSummary(
                request.getRequestId(),
                requester == null ? "System" : requester.getDisplayName(),
                requester == null ? "System" : normalizeRole(requester.getRoleEnum()),
                waferRepository.findByRequestRequestIdOrderByWaferCode(request.getRequestId()).stream()
                        .map(WaferEntity::getWaferCode)
                        .toList(),
                request.getExperiments().stream()
                        .map(ExperimentEntity::getExpName)
                        .toList(),
                request.getRemarks() == null ? "" : request.getRemarks(),
                request.getPriority(),
                request.getCreatedAt() == null ? "" : request.getCreatedAt().format(MANAGER_TIME_FORMAT)
        );
    }

    private List<String> normalizeWafers(List<String> waferIds) {
        if (waferIds == null || waferIds.isEmpty()) {
            throw badRequest("At least one wafer ID is required.");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String wafer : waferIds) {
            String value = required(wafer, "waferId").toUpperCase(Locale.ROOT);
            if (!value.matches("^W-[0-9]{4}$")) {
                throw badRequest("Wafer ID must match W-XXXX.");
            }
            normalized.add(value);
        }
        if (normalized.size() != waferIds.size()) {
            throw badRequest("Duplicate wafer IDs are not allowed.");
        }
        return new ArrayList<>(normalized);
    }

    private List<ExperimentEntity> normalizeExperiments(List<String> experimentKeys, String labId) {
        if (experimentKeys == null || experimentKeys.isEmpty()) {
            throw badRequest("At least one experiment is required.");
        }
        Set<String> uniqueKeys = new LinkedHashSet<>(experimentKeys);
        List<ExperimentEntity> experiments = experimentRepository.findByExpKeyIn(uniqueKeys);
        if (experiments.size() != uniqueKeys.size()) {
            throw badRequest("One or more experiments do not exist.");
        }
        boolean labMismatch = experiments.stream()
                .anyMatch(exp -> !exp.getLaboratory().getLabId().equals(labId));
        if (labMismatch) {
            throw badRequest("Selected experiments must belong to the target laboratory.");
        }
        return experiments.stream()
                .sorted(Comparator.comparing(ExperimentEntity::getExpKey))
                .toList();
    }

    private String normalizePriority(String priority) {
        String normalized = defaultIfBlank(priority, "NORMAL").toUpperCase(Locale.ROOT);
        if (!ALLOWED_PRIORITIES.contains(normalized)) {
            throw badRequest("Priority must be NORMAL, URGENT, or CRITICAL.");
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

    private String normalizeRole(String roleEnum) {
        if (roleEnum == null || roleEnum.isBlank()) {
            return "Unknown";
        }
        return switch (roleEnum) {
            case "ROLE_FAB_USER" -> "Fab User";
            case "ROLE_LAB_MANAGER" -> "Lab Manager";
            case "ROLE_LAB_OPERATOR" -> "Lab Operator";
            case "ROLE_MACHINE_OWNER" -> "Machine Owner";
            case "ROLE_SYSADMIN" -> "System Admin";
            default -> roleEnum;
        };
    }

    private void notifyRoles(Set<String> roleEnums, String title, String message, String type) {
        userRepository.findByRoleEnumInAndActiveTrue(roleEnums).stream()
                .map(user -> new NotificationEntity(user, title, message, type))
                .forEach(notificationRepository::save);
    }

    private void notifyRolesExcept(Set<String> roleEnums, String excludedEmployeeId, String title, String message, String type) {
        userRepository.findByRoleEnumInAndActiveTrue(roleEnums).stream()
                .filter(user -> !user.getEmployeeId().equals(excludedEmployeeId))
                .map(user -> new NotificationEntity(user, title, message, type))
                .forEach(notificationRepository::save);
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw badRequest(fieldName + " is required.");
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
