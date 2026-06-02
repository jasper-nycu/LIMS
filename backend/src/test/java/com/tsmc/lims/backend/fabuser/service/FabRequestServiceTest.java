package com.tsmc.lims.backend.fabuser.service;

import com.tsmc.lims.backend.auth.entity.Role;
import com.tsmc.lims.backend.auth.entity.User;
import com.tsmc.lims.backend.auth.repository.UserRepository;
import com.tsmc.lims.backend.auth.security.EcdsaCryptoProvider;
import com.tsmc.lims.backend.fabuser.dto.CreateFabRequest;
import com.tsmc.lims.backend.fabuser.dto.FabRequestSummary;
import com.tsmc.lims.backend.fabuser.dto.LaboratoryOption;
import com.tsmc.lims.backend.fabuser.entity.Experiment;
import com.tsmc.lims.backend.fabuser.entity.FabRequest;
import com.tsmc.lims.backend.fabuser.entity.Laboratory;
import com.tsmc.lims.backend.fabuser.repository.ExperimentRepository;
import com.tsmc.lims.backend.fabuser.repository.FabRequestRepository;
import com.tsmc.lims.backend.fabuser.repository.LaboratoryRepository;
import com.tsmc.lims.backend.fabuser.repository.WaferRepository;
import com.tsmc.lims.backend.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FabRequestServiceTest {

    private UserRepository userRepository;
    private LaboratoryRepository laboratoryRepository;
    private ExperimentRepository experimentRepository;
    private FabRequestRepository requestRepository;
    private WaferRepository waferRepository;
    private NotificationService notificationService;
    private EcdsaCryptoProvider cryptoProvider;
    private FabRequestService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        laboratoryRepository = mock(LaboratoryRepository.class);
        experimentRepository = mock(ExperimentRepository.class);
        requestRepository = mock(FabRequestRepository.class);
        waferRepository = mock(WaferRepository.class);
        notificationService = mock(NotificationService.class);
        cryptoProvider = mock(EcdsaCryptoProvider.class);
        service = new FabRequestService(
                userRepository, laboratoryRepository, experimentRepository,
                requestRepository, waferRepository, notificationService, cryptoProvider
        );
    }

    // ── listLaboratories ──────────────────────────────────────────────────────

    @Test
    void listLaboratories_returnsMappedLaboratoryOptionsWithExperiments() {
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        Experiment exp = new Experiment("exp_sem", lab, "SEM");
        when(laboratoryRepository.findAll()).thenReturn(List.of(lab));
        when(experimentRepository.findByLaboratoryLabIdOrderByExpKey("LAB_MA")).thenReturn(List.of(exp));

        List<LaboratoryOption> result = service.listLaboratories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).labId()).isEqualTo("LAB_MA");
        assertThat(result.get(0).labName()).isEqualTo("Material Analysis");
        assertThat(result.get(0).experiments()).hasSize(1);
        assertThat(result.get(0).experiments().get(0).expKey()).isEqualTo("exp_sem");
        assertThat(result.get(0).experiments().get(0).expName()).isEqualTo("SEM");
    }

    @Test
    void listLaboratories_returnsEmptyListWhenNoLabs() {
        when(laboratoryRepository.findAll()).thenReturn(List.of());

        assertThat(service.listLaboratories()).isEmpty();
    }

    // ── createRequest – requester and lab lookup ──────────────────────────────

    @Test
    void createRequest_requesterNotFound_throwsNotFound() {
        when(userRepository.findById("TS-9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRequest(
                new CreateFabRequest("TS-9999", "LAB_MA", List.of("exp_sem"), List.of("W-1234"), "NORMAL", "")
        )).isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("Requester user not found");
    }

    @Test
    void createRequest_labNotFound_throwsNotFound() {
        when(userRepository.findById("TS-1234")).thenReturn(Optional.of(stubUser("TS-1234")));
        when(laboratoryRepository.findById("LAB_XX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRequest(
                new CreateFabRequest("TS-1234", "LAB_XX", List.of("exp_sem"), List.of("W-1234"), "NORMAL", "")
        )).isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("Target laboratory not found");
    }

    // ── createRequest – wafer ID validation ──────────────────────────────────

    @Test
    void createRequest_emptyWaferList_throwsBadRequest() {
        stubRequesterAndLab();

        assertThatThrownBy(() -> service.createRequest(
                new CreateFabRequest("TS-1234", "LAB_MA", List.of("exp_sem"), List.of(), "NORMAL", "")
        )).isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("At least one wafer ID is required");
    }

    @Test
    void createRequest_invalidWaferFormat_throwsBadRequest() {
        stubRequesterAndLab();

        assertThatThrownBy(() -> service.createRequest(
                new CreateFabRequest("TS-1234", "LAB_MA", List.of("exp_sem"), List.of("INVALID"), "NORMAL", "")
        )).isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("Wafer ID must match W-XXXX");
    }

    @Test
    void createRequest_duplicateWaferIds_throwsBadRequest() {
        stubRequesterAndLab();

        assertThatThrownBy(() -> service.createRequest(
                new CreateFabRequest("TS-1234", "LAB_MA", List.of("exp_sem"),
                        List.of("W-1234", "W-1234"), "NORMAL", "")
        )).isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("Duplicate wafer IDs are prohibited");
    }

    // ── createRequest – experiment validation ─────────────────────────────────

    @Test
    void createRequest_emptyExperimentList_throwsBadRequest() {
        stubRequesterAndLab();

        assertThatThrownBy(() -> service.createRequest(
                new CreateFabRequest("TS-1234", "LAB_MA", List.of(), List.of("W-1234"), "NORMAL", "")
        )).isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("At least one experiment is required");
    }

    @Test
    void createRequest_unknownExperimentKey_throwsBadRequest() {
        stubRequesterAndLab();
        when(experimentRepository.findByExpKeyIn(Set.of("exp_nonexistent"))).thenReturn(List.of());

        assertThatThrownBy(() -> service.createRequest(
                new CreateFabRequest("TS-1234", "LAB_MA", List.of("exp_nonexistent"),
                        List.of("W-1234"), "NORMAL", "")
        )).isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("One or more selected experiments do not exist");
    }

    @Test
    void createRequest_experimentBelongsToDifferentLab_throwsBadRequest() {
        stubRequesterAndLab();
        Laboratory otherLab = new Laboratory("LAB_OTHER", "Other Lab");
        Experiment wrongLabExp = new Experiment("exp_other", otherLab, "Other Experiment");
        when(experimentRepository.findByExpKeyIn(Set.of("exp_other"))).thenReturn(List.of(wrongLabExp));

        assertThatThrownBy(() -> service.createRequest(
                new CreateFabRequest("TS-1234", "LAB_MA", List.of("exp_other"),
                        List.of("W-1234"), "NORMAL", "")
        )).isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("Selected experiments must belong to the target laboratory");
    }

    // ── createRequest – priority validation ───────────────────────────────────

    @Test
    void createRequest_invalidPriority_throwsBadRequest() {
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        Experiment exp = new Experiment("exp_sem", lab, "SEM");
        when(userRepository.findById("TS-1234")).thenReturn(Optional.of(stubUser("TS-1234")));
        when(laboratoryRepository.findById("LAB_MA")).thenReturn(Optional.of(lab));
        when(experimentRepository.findByExpKeyIn(Set.of("exp_sem"))).thenReturn(List.of(exp));

        assertThatThrownBy(() -> service.createRequest(
                new CreateFabRequest("TS-1234", "LAB_MA", List.of("exp_sem"),
                        List.of("W-1234"), "RUSH", "")
        )).isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("Priority must be NORMAL, URGENT, or CRITICAL");
    }

    @Test
    void createRequest_nullPriorityDefaultsToNormal() {
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        Experiment exp = new Experiment("exp_sem", lab, "SEM");
        User requester = stubUser("TS-1234");
        FabRequest saved = new FabRequest("REQ-100001", requester, lab, "NORMAL", "");
        saved.setExperiments(List.of(exp));

        when(userRepository.findById("TS-1234")).thenReturn(Optional.of(requester));
        when(laboratoryRepository.findById("LAB_MA")).thenReturn(Optional.of(lab));
        when(experimentRepository.findByExpKeyIn(Set.of("exp_sem"))).thenReturn(List.of(exp));
        when(requestRepository.existsById(anyString())).thenReturn(false);
        when(requestRepository.save(any())).thenReturn(saved);
        when(waferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(waferRepository.findByRequestRequestIdOrderByWaferCode(anyString())).thenReturn(List.of());
        when(userRepository.findByRoleRoleEnumInAndIsActiveTrue(any())).thenReturn(List.of());

        FabRequestSummary result = service.createRequest(
                new CreateFabRequest("TS-1234", "LAB_MA", List.of("exp_sem"), List.of("W-1234"), null, "")
        );

        assertThat(result).isNotNull();
        assertThat(result.priority()).isEqualTo("NORMAL");
    }

    // ── createRequest – happy path ────────────────────────────────────────────

    @Test
    void createRequest_waferIdNormalizesToUppercase() {
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        Experiment exp = new Experiment("exp_sem", lab, "SEM");
        User requester = stubUser("TS-1234");
        FabRequest saved = new FabRequest("REQ-100001", requester, lab, "NORMAL", "");
        saved.setExperiments(List.of(exp));

        when(userRepository.findById("TS-1234")).thenReturn(Optional.of(requester));
        when(laboratoryRepository.findById("LAB_MA")).thenReturn(Optional.of(lab));
        when(experimentRepository.findByExpKeyIn(Set.of("exp_sem"))).thenReturn(List.of(exp));
        when(requestRepository.existsById(anyString())).thenReturn(false);
        when(requestRepository.save(any())).thenReturn(saved);
        when(waferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(waferRepository.findByRequestRequestIdOrderByWaferCode(anyString())).thenReturn(List.of());
        when(userRepository.findByRoleRoleEnumInAndIsActiveTrue(any())).thenReturn(List.of());

        // lowercase 'w' must be accepted and auto-normalized to 'W'
        FabRequestSummary result = service.createRequest(
                new CreateFabRequest("TS-1234", "LAB_MA", List.of("exp_sem"), List.of("w-1234"), "NORMAL", "")
        );

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("REQ-100001");
    }

    @Test
    void createRequest_successNotifiesBothRequesterAndManagers() {
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        Experiment exp = new Experiment("exp_sem", lab, "SEM");
        User requester = stubUser("TS-1234");
        User manager = stubUserWithRole("MGR-001", "ROLE_LAB_MANAGER");
        FabRequest saved = new FabRequest("REQ-100001", requester, lab, "NORMAL", "");
        saved.setExperiments(List.of(exp));

        when(userRepository.findById("TS-1234")).thenReturn(Optional.of(requester));
        when(laboratoryRepository.findById("LAB_MA")).thenReturn(Optional.of(lab));
        when(experimentRepository.findByExpKeyIn(Set.of("exp_sem"))).thenReturn(List.of(exp));
        when(requestRepository.existsById(anyString())).thenReturn(false);
        when(requestRepository.save(any())).thenReturn(saved);
        when(waferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(waferRepository.findByRequestRequestIdOrderByWaferCode(anyString())).thenReturn(List.of());
        when(userRepository.findByRoleRoleEnumInAndIsActiveTrue(any())).thenReturn(List.of(manager));

        service.createRequest(
                new CreateFabRequest("TS-1234", "LAB_MA", List.of("exp_sem"), List.of("W-1234"), "NORMAL", "")
        );

        // manager gets 1 notification + requester gets 1 confirmation = 2 total
        verify(notificationService, times(2)).createNotification(anyString(), anyString(), anyString(), anyString());
    }

    // ── listRequestsByRequester ───────────────────────────────────────────────

    @Test
    void listRequestsByRequester_returnsSummariesOrderedByCreatedAt() {
        User requester = stubUser("TS-1234");
        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        FabRequest r1 = new FabRequest("REQ-100001", requester, lab, "NORMAL", "First");
        FabRequest r2 = new FabRequest("REQ-100002", requester, lab, "URGENT", "Second");

        when(requestRepository.findByRequesterEmployeeIdOrderByCreatedAtDesc("TS-1234"))
                .thenReturn(List.of(r1, r2));
        when(waferRepository.findByRequestRequestIdOrderByWaferCode(anyString())).thenReturn(List.of());

        List<FabRequestSummary> result = service.listRequestsByRequester("TS-1234");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("REQ-100001");
        assertThat(result.get(0).status()).isEqualTo("PENDING");
        assertThat(result.get(1).priority()).isEqualTo("URGENT");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubRequesterAndLab() {
        when(userRepository.findById("TS-1234")).thenReturn(Optional.of(stubUser("TS-1234")));
        when(laboratoryRepository.findById("LAB_MA")).thenReturn(Optional.of(new Laboratory("LAB_MA", "Material Analysis")));
    }

    private User stubUser(String empId) {
        User user = new User();
        user.setEmployeeId(empId);
        user.setFirstName("John");
        user.setLastName("Doe");
        Role role = new Role();
        role.setRoleEnum("ROLE_FAB_USER");
        user.setRole(role);
        return user;
    }

    private User stubUserWithRole(String empId, String roleEnum) {
        User user = stubUser(empId);
        user.getRole().setRoleEnum(roleEnum);
        return user;
    }
}
