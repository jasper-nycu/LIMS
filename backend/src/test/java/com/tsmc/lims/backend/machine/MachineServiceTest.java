package com.tsmc.lims.backend.machine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

import com.tsmc.lims.backend.machine.dto.DispatchRequest;
import com.tsmc.lims.backend.machine.dto.MachineResponse;
import com.tsmc.lims.backend.machine.dto.NameRequest;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MachineServiceTest {

    @Autowired
    private MachineService machineService;

    @Autowired
    private MachineRepository machineRepository;

    @BeforeEach
    void setUp() {
        machineRepository.deleteAll();
        MachineEntity machine = new MachineEntity("BAKE-OVEN-01", "LAB_RA", "High-Temp Bake", "exp_bake", MachineState.IDLE, 50, 0, null, 0, List.of("SC"));
        machineRepository.save(machine);
    }

    @Test
    void dispatchShouldMoveIdleToProcessingAndIncreaseLoadedCount() {
        DispatchRequest request = new DispatchRequest();
        request.setWaferIds(List.of("W-0001", "W-0002"));

        MachineResponse response = machineService.dispatch("BAKE-OVEN-01", request);

        assertThat(response.getState()).isEqualTo(MachineState.PROCESSING);
        assertThat(response.getLoadedCount()).isEqualTo(2);
        assertThat(response.getCurrentUtil()).isEqualTo(4);
    }

    @Test
    void dispatchShouldRejectWhenCapacityExceeded() {
        MachineEntity machine = machineRepository.findById("BAKE-OVEN-01").orElseThrow();
        machine.setLoadedCount(49);
        machineRepository.save(machine);

        DispatchRequest request = new DispatchRequest();
        request.setWaferIds(List.of("W-0001", "W-0002"));

        assertThatThrownBy(() -> machineService.dispatch("BAKE-OVEN-01", request))
                .isInstanceOf(MachineActionException.class)
                .hasMessageContaining("Total wafers exceed machine capacity");
    }

    @Test
    void toggleAlarmShouldMoveProcessingToAlarmAndBack() {
        MachineEntity machine = machineRepository.findById("BAKE-OVEN-01").orElseThrow();
        machine.setState(MachineState.PROCESSING);
        machine.setLoadedCount(3);
        machine.setCurrentUtil(6);
        machineRepository.save(machine);

        MachineResponse alarmResponse = machineService.toggleAlarm("BAKE-OVEN-01");
        assertThat(alarmResponse.getState()).isEqualTo(MachineState.ALARM);
        assertThat(alarmResponse.getError()).isEqualTo("ERR_SIMULATED_FAULT");
        assertThat(alarmResponse.getCurrentUtil()).isEqualTo(0);

        MachineResponse resolvedResponse = machineService.toggleAlarm("BAKE-OVEN-01");
        assertThat(resolvedResponse.getState()).isEqualTo(MachineState.PROCESSING);
        assertThat(resolvedResponse.getError()).isNull();
        assertThat(resolvedResponse.getCurrentUtil()).isEqualTo(6);
    }

    @Test
    void maintenanceShouldRejectWhileProcessing() {
        MachineEntity machine = machineRepository.findById("BAKE-OVEN-01").orElseThrow();
        machine.setState(MachineState.PROCESSING);
        machineRepository.save(machine);

        assertThatThrownBy(() -> machineService.toggleMaintenance("BAKE-OVEN-01"))
                .isInstanceOf(MachineActionException.class)
                .hasMessageContaining("Cannot enter maintenance while processing");
    }

    @Test
    void addRecipeShouldReturnUpdatedRecipeList() {
        NameRequest request = new NameRequest();
        request.setName("Bake-150C-4H");

        List<String> recipes = machineService.addRecipe("BAKE-OVEN-01", request);

        assertThat(recipes).contains("Bake-150C-4H");
    }
}
