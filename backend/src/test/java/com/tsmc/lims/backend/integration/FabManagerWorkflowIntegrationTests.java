package com.tsmc.lims.backend.integration;

import com.tsmc.lims.backend.auth.entity.Role;
import com.tsmc.lims.backend.auth.entity.User;
import com.tsmc.lims.backend.auth.repository.UserRepository;
import com.tsmc.lims.backend.fabuser.dto.CreateFabRequest;
import com.tsmc.lims.backend.fabuser.dto.FabRequestSummary;
import com.tsmc.lims.backend.fabuser.entity.Experiment;
import com.tsmc.lims.backend.fabuser.entity.Laboratory;
import com.tsmc.lims.backend.fabuser.repository.ExperimentRepository;
import com.tsmc.lims.backend.fabuser.repository.FabRequestRepository;
import com.tsmc.lims.backend.fabuser.repository.LaboratoryRepository;
import com.tsmc.lims.backend.fabuser.repository.WaferRepository;
import com.tsmc.lims.backend.fabuser.service.FabRequestService;
import com.tsmc.lims.backend.labmanager.service.LabManagerService;
import com.tsmc.lims.backend.labmanager.dto.LabWipSummary;
import com.tsmc.lims.backend.notification.repository.NotificationRepository;
import com.tsmc.lims.backend.support.PostgresTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FabManagerWorkflowIntegrationTests {

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        // 1. 原本容器的配置
        PostgresTestSupport.configure(registry, "lims_test_fab_workflow");
        
        // 2. 強制讓 Hibernate 自動建表時，直接蓋在 lims_test_fab_workflow 底下
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "lims_test_fab_workflow");
        
        // 3. 如果專案內有使用 Flyway 或 Liquibase 遷移工具，也一起強制指定
        registry.add("spring.flyway.schemas", () -> "lims_test_fab_workflow");
        registry.add("spring.liquibase.default-schema", () -> "lims_test_fab_workflow");
    }

    @Autowired private FabRequestService fabRequestService;
    @Autowired private LabManagerService labManagerService;
    @Autowired private UserRepository userRepository;
    @Autowired private LaboratoryRepository laboratoryRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private FabRequestRepository requestRepository;
    @Autowired private WaferRepository waferRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 確保 Schema 一定存在
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS lims_test_fab_workflow;");

        requestRepository.deleteAll();
        waferRepository.deleteAll();
        experimentRepository.deleteAll();
        laboratoryRepository.deleteAll();
        userRepository.deleteAll();
        notificationRepository.deleteAll();

        jdbcTemplate.execute("INSERT INTO roles (role_enum, role_name) VALUES ('ROLE_FAB_USER', 'Fab User') ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO roles (role_enum, role_name) VALUES ('ROLE_LAB_MANAGER', 'Lab Manager') ON CONFLICT DO NOTHING");

        Laboratory lab = new Laboratory("LAB_MA", "Material Analysis");
        laboratoryRepository.save(lab);

        experimentRepository.saveAll(List.of(
                new Experiment("exp_sem", lab, "SEM"),
                new Experiment("exp_xrd", lab, "XRD")
        ));

        Role roleUser = new Role();
        roleUser.setRoleEnum("ROLE_FAB_USER");
        User requester = new User();
        requester.setEmployeeId("TS-1234");
        requester.setRole(roleUser);
        requester.setFirstName("John");
        requester.setLastName("Doe");
        requester.setEmail("john@tsmc.com");
        requester.setPasswordHash("dummy_hash");
        requester.setPasswordSalt("dummy_salt");

        Role roleManager = new Role();
        roleManager.setRoleEnum("ROLE_LAB_MANAGER");
        User manager = new User();
        manager.setEmployeeId("TS-9001");
        manager.setRole(roleManager);
        manager.setFirstName("Jane");
        manager.setLastName("Doe");
        manager.setEmail("jane@tsmc.com");
        manager.setPasswordHash("dummy_hash");
        manager.setPasswordSalt("dummy_salt");

        userRepository.saveAll(List.of(requester, manager));
    }

    @Test
    void executeFullFabRequestLifecycleAndPrioritySorting() {
        FabRequestSummary normal = createSingleWaferRequest("W-7001", "NORMAL");
        labManagerService.approveRequest(normal.id(), "TS-9001");

        FabRequestSummary firstCritical = createSingleWaferRequest("W-7002", "CRITICAL");
        labManagerService.approveRequest(firstCritical.id(), "TS-9001");

        FabRequestSummary urgent = createSingleWaferRequest("W-7003", "URGENT");
        labManagerService.approveRequest(urgent.id(), "TS-9001");

        FabRequestSummary secondCritical = createSingleWaferRequest("W-7004", "CRITICAL");
        labManagerService.approveRequest(secondCritical.id(), "TS-9001");

        assertThat(labManagerService.listPendingWips())
                .extracting(LabWipSummary::priority)
                .containsExactly("CRITICAL", "CRITICAL", "URGENT", "NORMAL");
        
        assertThat(labManagerService.listPendingWips())
                .extracting(LabWipSummary::id)
                .satisfiesExactly(
                        id -> assertThat(id).startsWith("W-7002-"),
                        id -> assertThat(id).startsWith("W-7004-"),
                        id -> assertThat(id).startsWith("W-7003-"),
                        id -> assertThat(id).startsWith("W-7001-")
                );
    }

    private FabRequestSummary createSingleWaferRequest(String waferId, String priority) {
        return fabRequestService.createRequest(new CreateFabRequest(
                "TS-1234",
                "LAB_MA",
                List.of("exp_sem"),
                List.of(waferId),
                priority,
                "Test remark"
        ));
    }
}