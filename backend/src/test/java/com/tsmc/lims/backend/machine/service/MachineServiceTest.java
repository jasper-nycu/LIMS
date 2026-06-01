package com.tsmc.lims.backend.machine.service;

import com.tsmc.lims.backend.lab.dto.MachineResponse;
import com.tsmc.lims.backend.lab.entity.Machine;
import com.tsmc.lims.backend.lab.entity.WipTask;
import com.tsmc.lims.backend.lab.entity.enums.MachineState;
import com.tsmc.lims.backend.lab.entity.enums.WipStatus;
import com.tsmc.lims.backend.lab.repository.MachineRepository;
import com.tsmc.lims.backend.lab.repository.RecipeRepository;
import com.tsmc.lims.backend.lab.repository.WipTaskRepository;
import com.tsmc.lims.backend.fabuser.repository.FabRequestRepository;
import com.tsmc.lims.backend.lab.service.MachineLogService;
import com.tsmc.lims.backend.notification.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MachineServiceTest {

    @Test
    void findAll_returnsMachineResponseWithLiveWipCounts() {
        MachineRepository machineRepository    = mock(MachineRepository.class);
        WipTaskRepository wipTaskRepository    = mock(WipTaskRepository.class);
        RecipeRepository  recipeRepository     = mock(RecipeRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        MachineLogService machineLogService    = mock(MachineLogService.class);
        FabRequestRepository fabRequestRepository = mock(FabRequestRepository.class);

        com.tsmc.lims.backend.lab.service.MachineService service =
            new com.tsmc.lims.backend.lab.service.MachineService(
                machineRepository, wipTaskRepository, recipeRepository,
                notificationService, machineLogService, fabRequestRepository
            );

        // Build a stubbed Machine entity
        Machine machine = new Machine();
        machine.setMachineId("SEM-01");
        machine.setName("Surface Scan (SEM)");
        machine.setExpKey("exp_sem");
        machine.setState(MachineState.PROCESSING);
        machine.setCapacity(25);
        machine.setCurrentUtilization(72);
        machine.setOwners(List.of("MW", "JS"));

        // Build stubbed WipTasks (18 wafers in PROCESSING)
        List<WipTask> processingTasks = java.util.stream.IntStream.rangeClosed(1, 18)
            .mapToObj(i -> {
                WipTask t = new WipTask();
                t.setWaferCode("W-10" + String.format("%02d", i));
                t.setMachineId("SEM-01");
                t.setStatus(WipStatus.PROCESSING);
                return t;
            }).toList();

        when(machineRepository.findAllByOrderByMachineIdAsc()).thenReturn(List.of(machine));
        when(wipTaskRepository.findByMachineIdAndStatus("SEM-01", WipStatus.PROCESSING))
            .thenReturn(processingTasks);

        List<MachineResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        MachineResponse m = result.get(0);
        assertThat(m.getId()).isEqualTo("SEM-01");
        assertThat(m.getName()).isEqualTo("Surface Scan (SEM)");
        assertThat(m.getExpKey()).isEqualTo("exp_sem");
        assertThat(m.getState()).isEqualTo(MachineState.PROCESSING);
        assertThat(m.getCap()).isEqualTo(25);
        assertThat(m.getCurrentUtil()).isEqualTo(72);
        assertThat(m.getLoadedCount()).isEqualTo(18);
        assertThat(m.getLoadedWafers()).hasSize(18);
        assertThat(m.getOwners()).containsExactlyInAnyOrder("MW", "JS");
        assertThat(m.getError()).isNull();
    }
}
