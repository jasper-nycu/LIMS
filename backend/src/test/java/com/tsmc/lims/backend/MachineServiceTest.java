package com.tsmc.lims.backend;

import com.tsmc.lims.backend.fabuser.entity.Experiment;
import com.tsmc.lims.backend.fabuser.entity.FabRequest;
import com.tsmc.lims.backend.fabuser.entity.Laboratory;
import com.tsmc.lims.backend.fabuser.repository.LaboratoryRepository;
import com.tsmc.lims.backend.fabuser.repository.ExperimentRepository;
import com.tsmc.lims.backend.fabuser.repository.FabRequestRepository;
import com.tsmc.lims.backend.auth.repository.UserRepository;
import com.tsmc.lims.backend.auth.entity.User;
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

    @Autowired
    private FabRequestRepository fabRequestRepository;

    @Autowired
    private ExperimentRepository experimentRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String MACHINE_ID = "BAKE-OVEN-01";

    @BeforeEach
    void setUp() {
        // 1. 由子表往父表清理資料，避免外鍵衝突
        wipTaskRepository.deleteAll();
        machineRepository.deleteAll();

        // ===== 2. 建立並儲存 User (處理所有 nullable = false 的必填欄位) =====
        User requester = new User();
        requester.setEmployeeId("test_user");
        requester.setFirstName("First");
        requester.setLastName("Last");
        requester.setEmail("test@tsmc.com");
        requester.setPasswordHash("dummy_hash");
        requester.setPasswordSalt("dummy_salt");
        userRepository.save(requester);

        // ===== 3. 建立並儲存 Laboratory =====
        Laboratory lab = new Laboratory("LAB_RA", "Test Lab");
        laboratoryRepository.save(lab);

        // ===== 4. 建立並儲存 Experiment =====
        Experiment exp = new Experiment("exp_bake", lab, "Test Bake Exp");
        experimentRepository.save(exp);

        // ===== 5. 建立並儲存 FabRequest (解決 REQ-SYS-INIT 找不到的問題) =====
        FabRequest sysInitReq = new FabRequest("REQ-SYS-INIT", requester, lab, "NORMAL", "System Init");
        fabRequestRepository.save(sysInitReq);

        // ===== 6. 原本的 Machine 初始化 =====
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
        // 1. 先把機器狀態改成 PROCESSING (生產中不准直接去維修，才會觸發異常)
        Machine machine = machineRepository.findById(MACHINE_ID).orElseThrow();
        machine.setState(MachineState.PROCESSING); 
        machineRepository.save(machine);

        // 2. 執行測試斷言
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
