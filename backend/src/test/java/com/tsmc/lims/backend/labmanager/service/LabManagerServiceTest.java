package com.tsmc.lims.backend.labmanager.service;

import com.tsmc.lims.backend.auth.entity.Role;
import com.tsmc.lims.backend.auth.entity.User;
import com.tsmc.lims.backend.auth.repository.UserRepository;
import com.tsmc.lims.backend.auth.security.EcdsaCryptoProvider;
import com.tsmc.lims.backend.fabuser.entity.Experiment;
import com.tsmc.lims.backend.fabuser.entity.FabRequest;
import com.tsmc.lims.backend.fabuser.entity.Laboratory;
import com.tsmc.lims.backend.fabuser.entity.Wafer;
import com.tsmc.lims.backend.fabuser.repository.FabRequestRepository;
import com.tsmc.lims.backend.fabuser.repository.WaferRepository;
import com.tsmc.lims.backend.lab.entity.WipTask;
import com.tsmc.lims.backend.lab.entity.enums.WipStatus;
import com.tsmc.lims.backend.lab.repository.WipTaskRepository;
import com.tsmc.lims.backend.labmanager.dto.LabWipSummary;
import com.tsmc.lims.backend.labmanager.dto.ManagerRequestSummary;
import com.tsmc.lims.backend.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LabManagerServiceTest {

    private FabRequestRepository requestRepository;
    private WipTaskRepository wipTaskRepository;
    private UserRepository userRepository;
    private WaferRepository waferRepository;
    private NotificationService notificationService;
    private EcdsaCryptoProvider cryptoProvider;
    private LabManagerService service;

    @BeforeEach
    void setUp() {
        requestRepository = mock(FabRequestRepository.class);
        wipTaskRepository = mock(WipTaskRepository.class);
        userRepository = mock(UserRepository.class);
        waferRepository = mock(WaferRepository.class);
        notificationService = mock(NotificationService.class);
        cryptoProvider = mock(EcdsaCryptoProvider.class);
        service = new LabManagerService(
                requestRepository, wipTaskRepository, userRepository,
                waferRepository, notificationService, cryptoProvider
        );
    }

    // ── listPendingRequests ───────────────────────────────────────────────────

    @Test
    void listPendingRequests_returnsMappedSummaries() {
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        User requester = stubFabUser("TS-1234");
        FabRequest request = new FabRequest("REQ-100001", requester, lab, "NORMAL", "Test");
        request.setExperiments(List.of());

        when(requestRepository.findByStatusOrderedForManagerQueue("PENDING")).thenReturn(List.of(request));
        when(waferRepository.findByRequestRequestIdOrderByWaferCode("REQ-100001")).thenReturn(List.of());

        List<ManagerRequestSummary> result = service.listPendingRequests();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("REQ-100001");
        assertThat(result.get(0).requester()).isEqualTo("John Doe");
        assertThat(result.get(0).role()).isEqualTo("FAB_USER");
    }

    // ── approveRequest – error cases ──────────────────────────────────────────

    @Test
    void approveRequest_requestNotFound_throwsNotFound() {
        when(requestRepository.findById("REQ-MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveRequest("REQ-MISSING", "TS-9001"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Request item not found");
    }

    @Test
    void approveRequest_requestNotPending_throwsConflict() {
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        FabRequest request = new FabRequest("REQ-100001", stubFabUser("TS-1234"), lab, "NORMAL", "");
        request.approve(stubManagerUser("TS-9001"), null); // status → APPROVED
        when(requestRepository.findById("REQ-100001")).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approveRequest("REQ-100001", "TS-9001"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only PENDING requests can be processed");
    }

    @Test
    void approveRequest_approverNotFound_throwsNotFound() {
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        FabRequest request = new FabRequest("REQ-100001", stubFabUser("TS-1234"), lab, "NORMAL", "");
        when(requestRepository.findById("REQ-100001")).thenReturn(Optional.of(request));
        when(userRepository.findById("TS-GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveRequest("REQ-100001", "TS-GHOST"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Approver supervisor not found");
    }

    // ── approveRequest – happy path ───────────────────────────────────────────

    @Test
    void approveRequest_changesStatusToApprovedAndCreatesWipTasks() {
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        Experiment exp = new Experiment("exp_sem", lab, "SEM");
        User requester = stubFabUser("TS-1234");
        User approver = stubManagerUser("TS-9001");

        FabRequest request = new FabRequest("REQ-100001", requester, lab, "NORMAL", "");
        request.setExperiments(List.of(exp));

        Wafer wafer = new Wafer(request, "W-1234");

        when(requestRepository.findById("REQ-100001")).thenReturn(Optional.of(request));
        when(userRepository.findById("TS-9001")).thenReturn(Optional.of(approver));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(waferRepository.findByRequestRequestIdOrderByWaferCode("REQ-100001"))
                .thenReturn(List.of(wafer));
        when(wipTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ManagerRequestSummary result = service.approveRequest("REQ-100001", "TS-9001");

        assertThat(result.id()).isEqualTo("REQ-100001");
        // 1 wafer × 1 experiment = 1 WIP task created
        verify(wipTaskRepository, times(1)).save(any(WipTask.class));
        // Requester + approver each get a notification
        verify(notificationService, times(2)).createNotification(anyString(), anyString(), anyString(), anyString());
    }

    // ── rejectRequest – error cases ───────────────────────────────────────────

    @Test
    void rejectRequest_emptyRejectReason_throwsBadRequest() {
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        FabRequest request = new FabRequest("REQ-100001", stubFabUser("TS-1234"), lab, "NORMAL", "");
        when(requestRepository.findById("REQ-100001")).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.rejectRequest("REQ-100001", "TS-9001", ""))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Reject reason is required");
    }

    @Test
    void rejectRequest_requestNotPending_throwsConflict() {
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        FabRequest request = new FabRequest("REQ-100001", stubFabUser("TS-1234"), lab, "NORMAL", "");
        request.reject(stubManagerUser("TS-9001"), "Already rejected", null); // status → REJECTED
        when(requestRepository.findById("REQ-100001")).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.rejectRequest("REQ-100001", "TS-9001", "Some reason"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only PENDING requests can be processed");
    }

    // ── rejectRequest – happy path ────────────────────────────────────────────

    @Test
    void rejectRequest_changesStatusToRejectedAndNotifiesBothParties() {
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        User requester = stubFabUser("TS-1234");
        User approver = stubManagerUser("TS-9001");
        FabRequest request = new FabRequest("REQ-100001", requester, lab, "NORMAL", "");
        request.setExperiments(List.of());

        when(requestRepository.findById("REQ-100001")).thenReturn(Optional.of(request));
        when(userRepository.findById("TS-9001")).thenReturn(Optional.of(approver));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(waferRepository.findByRequestRequestIdOrderByWaferCode("REQ-100001")).thenReturn(List.of());

        service.rejectRequest("REQ-100001", "TS-9001", "Experiment not feasible.");

        verify(notificationService, times(2)).createNotification(anyString(), anyString(), anyString(), anyString());
        // No WIP tasks should be created for rejected requests
        verify(wipTaskRepository, never()).save(any());
    }

    // ── listPendingWips ───────────────────────────────────────────────────────

    @Test
    void listPendingWips_returnsMappedLabWipSummariesWithPriorityFromRequest() {
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        Experiment exp = new Experiment("exp_sem", lab, "SEM");
        User requester = stubFabUser("TS-1234");
        FabRequest fabRequest = new FabRequest("REQ-100001", requester, lab, "CRITICAL", "");

        WipTask task = new WipTask();
        task.setWaferCode("W-1234");
        task.setExpKey("exp_sem");
        task.setStatus(WipStatus.QUEUE);
        task.setExperiment(exp);
        task.setRequest(fabRequest);

        when(wipTaskRepository.findByStatusInOrderedForSorting(
                List.of(WipStatus.QUEUE, WipStatus.PENDING_SORTING)
        )).thenReturn(List.of(task));

        List<LabWipSummary> result = service.listPendingWips();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).waferCode()).isEqualTo("W-1234");
        assertThat(result.get(0).expKey()).isEqualTo("exp_sem");
        assertThat(result.get(0).priority()).isEqualTo("CRITICAL");
        assertThat(result.get(0).id()).contains("W-1234").contains("exp_sem");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User stubFabUser(String empId) {
        User user = new User();
        user.setEmployeeId(empId);
        user.setFirstName("John");
        user.setLastName("Doe");
        Role role = new Role();
        role.setRoleEnum("ROLE_FAB_USER");
        user.setRole(role);
        return user;
    }

    private User stubManagerUser(String empId) {
        User user = new User();
        user.setEmployeeId(empId);
        user.setFirstName("Jane");
        user.setLastName("Smith");
        Role role = new Role();
        role.setRoleEnum("ROLE_LAB_MANAGER");
        user.setRole(role);
        return user;
    }
}
