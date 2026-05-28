package com.tsmc.lims.backend;

import com.tsmc.lims.backend.lab.dto.DispatchRequest;
import com.tsmc.lims.backend.lab.dto.MachineResponse;
import com.tsmc.lims.backend.lab.dto.NameRequest;
import com.tsmc.lims.backend.lab.entity.Machine;
import com.tsmc.lims.backend.lab.entity.WipTask;
import com.tsmc.lims.backend.lab.entity.enums.MachineState;
import com.tsmc.lims.backend.lab.entity.enums.WipStatus;
import com.tsmc.lims.backend.lab.exception.InvalidStateTransitionException;
import com.tsmc.lims.backend.lab.repository.MachineRepository;
import com.tsmc.lims.backend.lab.repository.WipTaskRepository;
import com.tsmc.lims.backend.lab.service.MachineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MachineServiceTest {

    @Autowired
    private MachineService machineService;

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private WipTaskRepository wipTaskRepository;

    private static final String MACHINE_ID = "BAKE-OVEN-01";

    @BeforeEach
    void setUp() {
        wipTaskRepository.deleteAll();
        machineRepository.deleteAll();

        Machine machine = new Machine();
        machine.setMachineId(MACHINE_ID);
        machine.setLabId("LAB_RA");
        machine.setName("High-Temp Bake");
        machine.setExpKey("exp_bake");
        machine.setCapacity(50);
        machine.setState(MachineState.IDLE);
        machineRepository.save(machine);
    }

    @Test
    void dispatchShouldCreateWipTasksAndMoveToProcessing() {
        DispatchRequest req = new DispatchRequest();
        req.setWaferCodes(List.of("W-0001", "W-0002"));
        req.setRecipeId("Bake-150C-4H");

        MachineResponse response = machineService.dispatch(MACHINE_ID, req);

        assertThat(response.getState()).isEqualTo(MachineState.PROCESSING);
        assertThat(response.getLoadedCount()).isEqualTo(2);
        assertThat(response.getCurrentUtil()).isEqualTo(4); // 2/50 * 100 = 4
    }

    @Test
    void dispatchShouldRejectWhenMachineNotIdle() {
        for (int i = 1; i <= 49; i++) {
            WipTask t = new WipTask();
            t.setWaferCode("W-PRE-" + i);
            t.setMachineId(MACHINE_ID);
            t.setStatus(WipStatus.PROCESSING);
            wipTaskRepository.save(t);
        }
        Machine m = machineRepository.findById(MACHINE_ID).orElseThrow();
        m.setState(MachineState.PROCESSING);
        machineRepository.save(m);

        DispatchRequest req = new DispatchRequest();
        req.setWaferCodes(List.of("W-0001", "W-0002"));
        req.setRecipeId("Bake-150C-4H");

        assertThatThrownBy(() -> machineService.dispatch(MACHINE_ID, req))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void simulateErrorShouldMoveProcessingToAlarm() {
        DispatchRequest req = new DispatchRequest();
        req.setWaferCodes(List.of("W-0001"));
        req.setRecipeId("Bake-150C-4H");
        machineService.dispatch(MACHINE_ID, req);

        MachineResponse response = machineService.simulateError(MACHINE_ID);

        assertThat(response.getState()).isEqualTo(MachineState.ALARM);
        assertThat(response.getError()).isEqualTo("ERR_SIMULATED_FAULT");
        assertThat(response.getCurrentUtil()).isEqualTo(0);
    }

    @Test
    void resolveAlarmShouldRestoreProcessingWhenWafersStillLoaded() {
        DispatchRequest req = new DispatchRequest();
        req.setWaferCodes(List.of("W-0001", "W-0002", "W-0003"));
        req.setRecipeId("Bake-150C-4H");
        machineService.dispatch(MACHINE_ID, req);
        machineService.simulateError(MACHINE_ID);

        MachineResponse response = machineService.resolveAlarm(MACHINE_ID);

        assertThat(response.getState()).isEqualTo(MachineState.PROCESSING);
        assertThat(response.getError()).isNull();
    }

    @Test
    void toMaintenanceShouldRequireAlarmState() {
        assertThatThrownBy(() -> machineService.toMaintenance(MACHINE_ID))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void addRecipeShouldReturnUpdatedRecipeList() {
        NameRequest request = new NameRequest();
        request.setName("Bake-150C-4H");

        List<String> recipes = machineService.addRecipe(MACHINE_ID, request);

        assertThat(recipes).contains("Bake-150C-4H");
    }
}
