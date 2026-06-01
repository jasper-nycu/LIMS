package com.tsmc.lims.backend;

import com.tsmc.lims.backend.auth.entity.Role;
import com.tsmc.lims.backend.auth.entity.User;
import com.tsmc.lims.backend.auth.repository.RoleRepository;
import com.tsmc.lims.backend.auth.repository.UserRepository;
import com.tsmc.lims.backend.lab.dto.DispatchRequest;
import com.tsmc.lims.backend.lab.dto.EmgUnloadRequest;
import com.tsmc.lims.backend.lab.dto.MachineResponse;
import com.tsmc.lims.backend.lab.entity.Machine;
import com.tsmc.lims.backend.lab.entity.WipTask;
import com.tsmc.lims.backend.lab.entity.enums.MachineState;
import com.tsmc.lims.backend.lab.entity.enums.WipStatus;
import com.tsmc.lims.backend.lab.exception.InvalidStateTransitionException;
import com.tsmc.lims.backend.lab.repository.MachineRepository;
import com.tsmc.lims.backend.lab.repository.WipTaskRepository;
import com.tsmc.lims.backend.lab.service.MachineService;
import com.tsmc.lims.backend.notification.entity.Notification;
import com.tsmc.lims.backend.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LabOperationsFsmTest {

    @Autowired MachineService machineService;
    @Autowired MachineRepository machineRepository;
    @Autowired WipTaskRepository wipTaskRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;

    private static final String MACHINE_ID  = "BAKE-OVEN-01";
    private static final String OWNER_ID    = "EMP-OWNER-01";
    private static final String MANAGER_ID  = "EMP-MGR-01";
    private static final String OPERATOR_ID = "EMP-OPR-01";
    private static final String FAB_ID      = "EMP-FAB-01";

    @BeforeEach
    void setUp() {
        roleRepository.save(role("ROLE_MACHINE_OWNER", "Machine Owner"));
        roleRepository.save(role("ROLE_LAB_MANAGER",   "Lab Supervisor"));
        roleRepository.save(role("ROLE_LAB_OPERATOR",  "Lab Operator"));
        roleRepository.save(role("ROLE_FAB_USER",      "Fab User"));

        userRepository.save(user(OWNER_ID,    "ROLE_MACHINE_OWNER"));
        userRepository.save(user(MANAGER_ID,  "ROLE_LAB_MANAGER"));
        userRepository.save(user(OPERATOR_ID, "ROLE_LAB_OPERATOR"));
        userRepository.save(user(FAB_ID,      "ROLE_FAB_USER"));

        Machine m = new Machine();
        m.setMachineId(MACHINE_ID);
        m.setLabId("LAB_RA");
        m.setName("High-Temp Bake");
        m.setExpKey("exp_bake");
        m.setCapacity(50);
        m.setState(MachineState.IDLE);
        m.setCurrentUtilization(0);
        machineRepository.save(m);

        for (int i = 1; i <= 3; i++) {
            WipTask t = new WipTask();
            t.setRequestId("REQ-TEST-001");
            t.setWaferCode("W-TEST-00" + i);
            t.setExpKey("exp_bake");
            t.setStatus(WipStatus.QUEUE);
            t.setPriority("NORMAL");
            wipTaskRepository.save(t);
        }
    }

    private Role role(String roleEnum, String roleName) {
        Role r = new Role();
        r.setRoleEnum(roleEnum);
        r.setRoleName(roleName);
        return r;
    }

    private User user(String empId, String roleEnum) {
        User u = new User();
        u.setEmployeeId(empId);
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmail(empId.toLowerCase() + "@test.com");
        u.setPasswordHash("hash");
        u.setPasswordSalt("salt");
        u.setIsActive(true);
        u.setRole(roleRepository.getReferenceById(roleEnum));
        return u;
    }

    private MachineResponse dispatchTestWafers() {
        DispatchRequest req = new DispatchRequest();
        req.setMachineId(MACHINE_ID);
        req.setRecipeId("Bake-150C-4H");
        req.setWaferCodes(List.of("W-TEST-001", "W-TEST-002", "W-TEST-003"));
        req.setRequestId("REQ-TEST-001");
        req.setExpKey("exp_bake");
        return machineService.dispatch(MACHINE_ID, req);
    }

    private Notification latestNotifFor(String userId) {
        List<Notification> notifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(notifs).as("Notifications for " + userId).isNotEmpty();
        return notifs.get(0);
    }

    // ── API 1: Dispatch ──────────────────────────────────────────────────────

    @Test
    @DisplayName("API-1: Dispatch → Machine=PROCESSING, Wafers=PROCESSING, wafer-roles notified with SUCCESS")
    void testDispatch() {
        MachineResponse response = dispatchTestWafers();

        assertThat(response.getState()).isEqualTo(MachineState.PROCESSING);
        assertThat(wipTaskRepository.findByMachineIdAndStatus(MACHINE_ID, WipStatus.PROCESSING)).hasSize(3);

        assertThat(latestNotifFor(MANAGER_ID).getType()).isEqualTo("success");
        assertThat(latestNotifFor(OPERATOR_ID).getType()).isEqualTo("success");
        // FAB_USER is not in WAFER_ROLES and setUp has no FabRequest, so no individual notification
        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(OWNER_ID)).isEmpty();
    }

    // ── Flow 1: Safe Unload ──────────────────────────────────────────────────

    @Test
    @DisplayName("Flow-1: Unload → Machine=IDLE, Wafers=COMPLETED, wafer-roles notified with SUCCESS")
    void testSafeUnload() {
        dispatchTestWafers();
        MachineResponse response = machineService.unload(MACHINE_ID);

        assertThat(response.getState()).isEqualTo(MachineState.IDLE);
        assertThat(response.getCurrentUtil()).isEqualTo(0);
        assertThat(wipTaskRepository.findByMachineIdAndStatus(MACHINE_ID, WipStatus.COMPLETED)).hasSize(3);

        assertThat(latestNotifFor(MANAGER_ID).getType()).isEqualTo("success");
        assertThat(latestNotifFor(OPERATOR_ID).getType()).isEqualTo("success");
        // FAB_USER notified via notifyRequesters only when a FabRequest exists in DB
    }

    // ── Flow 2: EMG Unload — REUSE ───────────────────────────────────────────

    @Test
    @DisplayName("Flow-2: EMG Unload REUSE → Machine=IDLE, Wafers=PENDING_SORTING, wafer-roles notified with INFO")
    void testEmgUnloadReuse() {
        dispatchTestWafers();
        MachineResponse response = machineService.emgUnload(MACHINE_ID, EmgUnloadRequest.Action.REUSE);

        assertThat(response.getState()).isEqualTo(MachineState.IDLE);
        assertThat(wipTaskRepository.findByStatus(WipStatus.PENDING_SORTING)).hasSize(3);

        assertThat(latestNotifFor(MANAGER_ID).getType()).isEqualTo("info");
        // FAB_USER notified via notifyRequesters only when a FabRequest exists in DB
    }

    // ── Flow 3: EMG Unload — SCRAP ───────────────────────────────────────────

    @Test
    @DisplayName("Flow-3: EMG Unload SCRAP → Machine=IDLE, Wafers=SCRAPPED, wafer-roles notified with WARNING")
    void testEmgUnloadScrap() {
        dispatchTestWafers();
        MachineResponse response = machineService.emgUnload(MACHINE_ID, EmgUnloadRequest.Action.SCRAP);

        assertThat(response.getState()).isEqualTo(MachineState.IDLE);
        assertThat(wipTaskRepository.findByStatus(WipStatus.SCRAPPED)).hasSize(3);

        assertThat(latestNotifFor(MANAGER_ID).getType()).isEqualTo("warning");
        // FAB_USER notified via notifyRequesters only when a FabRequest exists in DB
    }

    // ── Flow 4: Simulate Error → Resolve Alarm ───────────────────────────────

    @Test
    @DisplayName("Flow-4A: SimulateError → Machine=ALARM, machine owner notified with ERROR")
    void testSimulateError() {
        dispatchTestWafers();
        MachineResponse response = machineService.simulateError(MACHINE_ID);

        assertThat(response.getState()).isEqualTo(MachineState.ALARM);
        assertThat(response.getError()).isEqualTo("ERR_SIMULATED_FAULT");

        assertThat(latestNotifFor(OWNER_ID).getType()).isEqualTo("error");
        // MANAGER_ID is not in MACHINE_ROLES, so latest notification is still from dispatch (success)
        assertThat(latestNotifFor(MANAGER_ID).getType()).isEqualTo("success");
    }

    @Test
    @DisplayName("Flow-4B: ResolveAlarm → Machine=PROCESSING, machine owner notified with SUCCESS")
    void testResolveAlarm() {
        dispatchTestWafers();
        machineService.simulateError(MACHINE_ID);
        MachineResponse response = machineService.resolveAlarm(MACHINE_ID);

        assertThat(response.getState()).isEqualTo(MachineState.PROCESSING);
        assertThat(response.getError()).isNull();
        assertThat(latestNotifFor(OWNER_ID).getType()).isEqualTo("success");
    }

    // ── Flow 5: Simulate Error → Maintenance → Set Online ────────────────────

    @Test
    @DisplayName("Flow-5B: ALARM → MAINTENANCE, machine owner notified with INFO")
    void testToMaintenance() {
        dispatchTestWafers();
        machineService.simulateError(MACHINE_ID);
        MachineResponse response = machineService.toMaintenance(MACHINE_ID);

        assertThat(response.getState()).isEqualTo(MachineState.MAINTENANCE);
        assertThat(latestNotifFor(OWNER_ID).getType()).isEqualTo("info");
    }

    @Test
    @DisplayName("Flow-5C: MAINTENANCE → PROCESSING, machine owner notified with SUCCESS")
    void testSetOnlineFromMaintenance() {
        dispatchTestWafers();
        machineService.simulateError(MACHINE_ID);
        machineService.toMaintenance(MACHINE_ID);
        MachineResponse response = machineService.setOnline(MACHINE_ID);

        assertThat(response.getState()).isEqualTo(MachineState.PROCESSING);
        assertThat(response.getError()).isNull();
        assertThat(latestNotifFor(OWNER_ID).getType()).isEqualTo("success");
    }

    // ── Guards ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Guard: Cannot simulate error on IDLE machine")
    void testCannotSimulateErrorOnIdle() {
        assertThatThrownBy(() -> machineService.simulateError(MACHINE_ID))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("Guard: Cannot unload a machine that is in ALARM state")
    void testCannotUnloadAlarmMachine() {
        dispatchTestWafers();
        machineService.simulateError(MACHINE_ID);
        assertThatThrownBy(() -> machineService.unload(MACHINE_ID))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("Guard: 404 when machine does not exist")
    void testMachineNotFound() {
        assertThatThrownBy(() -> machineService.findById("NON-EXISTENT"))
                .hasMessageContaining("Machine not found");
    }
}
