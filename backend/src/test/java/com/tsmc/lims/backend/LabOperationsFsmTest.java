package com.tsmc.lims.backend;

import com.tsmc.lims.backend.lab.dto.DispatchRequest;
import com.tsmc.lims.backend.lab.dto.EmgUnloadRequest;
import com.tsmc.lims.backend.lab.dto.MachineResponse;
import com.tsmc.lims.backend.lab.entity.Machine;
import com.tsmc.lims.backend.lab.entity.Notification;
import com.tsmc.lims.backend.lab.entity.WipTask;
import com.tsmc.lims.backend.lab.entity.enums.MachineState;
import com.tsmc.lims.backend.lab.entity.enums.NotificationType;
import com.tsmc.lims.backend.lab.entity.enums.WipStatus;
import com.tsmc.lims.backend.lab.exception.InvalidStateTransitionException;
import com.tsmc.lims.backend.lab.repository.MachineRepository;
import com.tsmc.lims.backend.lab.repository.NotificationRepository;
import com.tsmc.lims.backend.lab.repository.WipTaskRepository;
import com.tsmc.lims.backend.lab.service.MachineService;
import com.tsmc.lims.backend.lab.service.NotificationService;
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
    @Autowired NotificationService notificationService;
    @Autowired MachineRepository machineRepository;
    @Autowired WipTaskRepository wipTaskRepository;
    @Autowired NotificationRepository notificationRepository;

    private static final String MACHINE_ID = "BAKE-OVEN-01";

    @BeforeEach
    void setUp() {
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

    private MachineResponse dispatchTestWafers() {
        DispatchRequest req = new DispatchRequest();
        req.setMachineId(MACHINE_ID);
        req.setRecipeId("Bake-150C-4H");
        req.setWaferCodes(List.of("W-TEST-001", "W-TEST-002", "W-TEST-003"));
        req.setRequestId("REQ-TEST-001");
        req.setExpKey("exp_bake");
        return machineService.dispatch(MACHINE_ID, req);
    }

    // ── API 1: Dispatch ──────────────────────────────────────────────────────

    @Test
    @DisplayName("API-1: Dispatch → Machine=PROCESSING, Wafers=PROCESSING, Notification=SUCCESS")
    void testDispatch() {
        MachineResponse response = dispatchTestWafers();

        assertThat(response.getState()).isEqualTo(MachineState.PROCESSING);

        List<WipTask> processing = wipTaskRepository.findByMachineIdAndStatus(MACHINE_ID, WipStatus.PROCESSING);
        assertThat(processing).hasSize(3);

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.SUCCESS);
    }

    // ── Flow 1: Safe Unload ──────────────────────────────────────────────────

    @Test
    @DisplayName("Flow-1: Unload → Machine=IDLE, Wafers=COMPLETED, Notification=SUCCESS")
    void testSafeUnload() {
        dispatchTestWafers();

        MachineResponse response = machineService.unload(MACHINE_ID);

        assertThat(response.getState()).isEqualTo(MachineState.IDLE);
        assertThat(response.getCurrentUtil()).isEqualTo(0);

        List<WipTask> completed = wipTaskRepository.findByMachineIdAndStatus(MACHINE_ID, WipStatus.COMPLETED);
        assertThat(completed).hasSize(3);

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.SUCCESS);
    }

    // ── Flow 2: EMG Unload — REUSE ───────────────────────────────────────────

    @Test
    @DisplayName("Flow-2: EMG Unload REUSE → Machine=IDLE, Wafers=PENDING_SORTING, Notification=INFO")
    void testEmgUnloadReuse() {
        dispatchTestWafers();

        MachineResponse response = machineService.emgUnload(MACHINE_ID, EmgUnloadRequest.Action.REUSE);

        assertThat(response.getState()).isEqualTo(MachineState.IDLE);

        List<WipTask> pending = wipTaskRepository.findByStatus(WipStatus.PENDING_SORTING);
        assertThat(pending).hasSize(3);

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.INFO);
    }

    // ── Flow 3: EMG Unload — SCRAP ───────────────────────────────────────────

    @Test
    @DisplayName("Flow-3: EMG Unload SCRAP → Machine=IDLE, Wafers=SCRAPPED, Notification=WARNING")
    void testEmgUnloadScrap() {
        dispatchTestWafers();

        MachineResponse response = machineService.emgUnload(MACHINE_ID, EmgUnloadRequest.Action.SCRAP);

        assertThat(response.getState()).isEqualTo(MachineState.IDLE);

        List<WipTask> scrapped = wipTaskRepository.findByStatus(WipStatus.SCRAPPED);
        assertThat(scrapped).hasSize(3);

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.WARNING);
    }

    // ── Flow 4: Simulate Error → Resolve Alarm ───────────────────────────────

    @Test
    @DisplayName("Flow-4A: SimulateError → Machine=ALARM, Notification=ERROR")
    void testSimulateError() {
        dispatchTestWafers();

        MachineResponse response = machineService.simulateError(MACHINE_ID);

        assertThat(response.getState()).isEqualTo(MachineState.ALARM);
        assertThat(response.getError()).isEqualTo("ERR_SIMULATED_FAULT");

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.ERROR);
    }

    @Test
    @DisplayName("Flow-4B: ResolveAlarm → Machine=PROCESSING (wafers still loaded), Notification=SUCCESS")
    void testResolveAlarm() {
        dispatchTestWafers();
        machineService.simulateError(MACHINE_ID);

        MachineResponse response = machineService.resolveAlarm(MACHINE_ID);

        assertThat(response.getState()).isEqualTo(MachineState.PROCESSING);
        assertThat(response.getError()).isNull();

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.SUCCESS);
    }

    // ── Flow 5: Simulate Error → Maintenance → Set Online ────────────────────

    @Test
    @DisplayName("Flow-5B: ALARM → MAINTENANCE, Notification=INFO")
    void testToMaintenance() {
        dispatchTestWafers();
        machineService.simulateError(MACHINE_ID);

        MachineResponse response = machineService.toMaintenance(MACHINE_ID);

        assertThat(response.getState()).isEqualTo(MachineState.MAINTENANCE);

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.INFO);
    }

    @Test
    @DisplayName("Flow-5C: MAINTENANCE → PROCESSING (wafers still loaded), Notification=SUCCESS")
    void testSetOnlineFromMaintenance() {
        dispatchTestWafers();
        machineService.simulateError(MACHINE_ID);
        machineService.toMaintenance(MACHINE_ID);

        MachineResponse response = machineService.setOnline(MACHINE_ID);

        assertThat(response.getState()).isEqualTo(MachineState.PROCESSING);
        assertThat(response.getError()).isNull();

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.SUCCESS);
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
