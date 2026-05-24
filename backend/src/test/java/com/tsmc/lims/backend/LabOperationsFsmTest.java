package com.tsmc.lims.backend;

import com.tsmc.lims.backend.dto.DispatchRequest;
import com.tsmc.lims.backend.dto.EmgUnloadRequest;
import com.tsmc.lims.backend.entity.Machine;
import com.tsmc.lims.backend.entity.Notification;
import com.tsmc.lims.backend.entity.WipTask;
import com.tsmc.lims.backend.entity.enums.MachineState;
import com.tsmc.lims.backend.entity.enums.NotificationType;
import com.tsmc.lims.backend.entity.enums.WipStatus;
import com.tsmc.lims.backend.exception.InvalidStateTransitionException;
import com.tsmc.lims.backend.repository.MachineRepository;
import com.tsmc.lims.backend.repository.NotificationRepository;
import com.tsmc.lims.backend.repository.WipTaskRepository;
import com.tsmc.lims.backend.service.DispatchService;
import com.tsmc.lims.backend.service.MachineStateService;
import com.tsmc.lims.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class LabOperationsFsmTest {

    @Autowired DispatchService dispatchService;
    @Autowired MachineStateService machineStateService;
    @Autowired NotificationService notificationService;
    @Autowired MachineRepository machineRepository;
    @Autowired WipTaskRepository wipTaskRepository;
    @Autowired NotificationRepository notificationRepository;

    private static final String MACHINE_ID = "BAKE-OVEN-01";

    @BeforeEach
    void setUp() {
        // Seed a test machine
        Machine m = new Machine();
        m.setMachineId(MACHINE_ID);
        m.setLabId("LAB_RA");
        m.setName("High-Temp Bake");
        m.setCapacity(50);
        m.setState(MachineState.IDLE);
        m.setCurrentUtilization(0);
        machineRepository.save(m);

        // Seed 3 wafer tasks in QUEUE status
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

    // -----------------------------------------------------------------------
    // Helper: dispatch 3 test wafers to BAKE-OVEN-01
    // -----------------------------------------------------------------------
    private Machine dispatchTestWafers() {
        DispatchRequest req = new DispatchRequest();
        req.setMachineId(MACHINE_ID);
        req.setRecipeId("Bake-150C-4H");
        req.setWaferCodes(List.of("W-TEST-001", "W-TEST-002", "W-TEST-003"));
        req.setRequestId("REQ-TEST-001");
        req.setExpKey("exp_bake");
        return dispatchService.dispatch(req);
    }

    // -----------------------------------------------------------------------
    // API 1: Dispatch
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("API-1: Dispatch → Machine=PROCESSING, Wafers=PROCESSING, Notification=INFO")
    void testDispatch() {
        Machine machine = dispatchTestWafers();

        // Assert machine state
        assertThat(machine.getState()).isEqualTo(MachineState.PROCESSING);

        // Assert wafer statuses in DB
        List<WipTask> processing = wipTaskRepository.findByMachineIdAndStatus(MACHINE_ID, WipStatus.PROCESSING);
        assertThat(processing).hasSize(3);

        // Assert notification type
        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.INFO);
    }

    // -----------------------------------------------------------------------
    // Flow 1: Safe Unload
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Flow-1: Unload → Machine=IDLE, Wafers=COMPLETED, Notification=SUCCESS")
    void testSafeUnload() {
        dispatchTestWafers();

        Machine machine = machineStateService.unload(MACHINE_ID);

        // Assert machine back to IDLE
        assertThat(machine.getState()).isEqualTo(MachineState.IDLE);
        assertThat(machine.getCurrentUtilization()).isEqualTo(0);

        // Assert all wafers COMPLETED
        List<WipTask> completed = wipTaskRepository.findByMachineIdAndStatus(MACHINE_ID, WipStatus.COMPLETED);
        assertThat(completed).hasSize(3);

        // Assert SUCCESS notification
        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.SUCCESS);
    }

    // -----------------------------------------------------------------------
    // Flow 2: EMG Unload — REUSE
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Flow-2: EMG Unload REUSE → Machine=IDLE, Wafers=PENDING_SORTING, Notification=WARNING")
    void testEmgUnloadReuse() {
        dispatchTestWafers();

        Machine machine = machineStateService.emgUnload(MACHINE_ID, EmgUnloadRequest.Action.REUSE);

        assertThat(machine.getState()).isEqualTo(MachineState.IDLE);

        // Wafers must be back in WIP as PENDING_SORTING
        List<WipTask> pending = wipTaskRepository.findByStatus(WipStatus.PENDING_SORTING);
        assertThat(pending).hasSize(3);

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.WARNING);
    }

    // -----------------------------------------------------------------------
    // Flow 3: EMG Unload — SCRAP
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Flow-3: EMG Unload SCRAP → Machine=IDLE, Wafers=SCRAPPED, Notification=WARNING")
    void testEmgUnloadScrap() {
        dispatchTestWafers();

        Machine machine = machineStateService.emgUnload(MACHINE_ID, EmgUnloadRequest.Action.SCRAP);

        assertThat(machine.getState()).isEqualTo(MachineState.IDLE);

        List<WipTask> scrapped = wipTaskRepository.findByStatus(WipStatus.SCRAPPED);
        assertThat(scrapped).hasSize(3);

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.WARNING);
    }

    // -----------------------------------------------------------------------
    // Flow 4: Simulate Error → Resolve Alarm
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Flow-4A: SimulateError → Machine=ALARM, Notification=ERROR")
    void testSimulateError() {
        dispatchTestWafers();

        Machine machine = machineStateService.simulateError(MACHINE_ID);

        assertThat(machine.getState()).isEqualTo(MachineState.ALARM);
        assertThat(machine.getErrorCode()).isEqualTo("ERR_SIMULATED_FAULT");

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.ERROR);
    }

    @Test
    @DisplayName("Flow-4B: ResolveAlarm → Machine=PROCESSING (wafers still loaded), Notification=SUCCESS")
    void testResolveAlarm() {
        dispatchTestWafers();
        machineStateService.simulateError(MACHINE_ID);

        Machine machine = machineStateService.resolveAlarm(MACHINE_ID);

        // Wafers are still loaded → back to PROCESSING
        assertThat(machine.getState()).isEqualTo(MachineState.PROCESSING);
        assertThat(machine.getErrorCode()).isNull();

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.SUCCESS);
    }

    // -----------------------------------------------------------------------
    // Flow 5: Simulate Error → Maintenance → Set Online
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Flow-5B: ALARM → MAINTENANCE, Notification=WARNING")
    void testToMaintenance() {
        dispatchTestWafers();
        machineStateService.simulateError(MACHINE_ID);

        Machine machine = machineStateService.toMaintenance(MACHINE_ID);

        assertThat(machine.getState()).isEqualTo(MachineState.MAINTENANCE);

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.WARNING);
    }

    @Test
    @DisplayName("Flow-5C: MAINTENANCE → PROCESSING (wafers still loaded), Notification=SUCCESS")
    void testSetOnlineFromMaintenance() {
        dispatchTestWafers();
        machineStateService.simulateError(MACHINE_ID);
        machineStateService.toMaintenance(MACHINE_ID);

        Machine machine = machineStateService.setOnline(MACHINE_ID);

        // Wafers still in PROCESSING → machine back to PROCESSING
        assertThat(machine.getState()).isEqualTo(MachineState.PROCESSING);
        assertThat(machine.getErrorCode()).isNull();

        List<Notification> notifications = notificationRepository.findByMachineIdOrderByCreatedAtDesc(MACHINE_ID);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.SUCCESS);
    }

    // -----------------------------------------------------------------------
    // Guard: invalid transitions must throw
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Guard: Cannot simulate error on IDLE machine")
    void testCannotSimulateErrorOnIdle() {
        assertThatThrownBy(() -> machineStateService.simulateError(MACHINE_ID))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("Guard: Cannot unload a machine that is in ALARM state")
    void testCannotUnloadAlarmMachine() {
        dispatchTestWafers();
        machineStateService.simulateError(MACHINE_ID);

        assertThatThrownBy(() -> machineStateService.unload(MACHINE_ID))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("Guard: 404 when machine does not exist")
    void testMachineNotFound() {
        assertThatThrownBy(() -> machineStateService.findById("NON-EXISTENT"))
                .hasMessageContaining("Machine not found");
    }
}
