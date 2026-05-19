package com.tsmc.lims.backend;

import com.tsmc.lims.backend.domain.ExperimentEntity;
import com.tsmc.lims.backend.domain.LaboratoryEntity;
import com.tsmc.lims.backend.domain.UserEntity;
import com.tsmc.lims.backend.dto.CreateFabRequest;
import com.tsmc.lims.backend.dto.FabRequestSummary;
import com.tsmc.lims.backend.dto.ManagerRequestSummary;
import com.tsmc.lims.backend.repository.ExperimentRepository;
import com.tsmc.lims.backend.repository.FabRequestRepository;
import com.tsmc.lims.backend.repository.LaboratoryRepository;
import com.tsmc.lims.backend.repository.NotificationRepository;
import com.tsmc.lims.backend.repository.UserRepository;
import com.tsmc.lims.backend.repository.WaferRepository;
import com.tsmc.lims.backend.repository.WipTaskRepository;
import com.tsmc.lims.backend.service.FabManagerService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:lims_workflow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class FabManagerWorkflowIntegrationTests {

    @Autowired
    private FabManagerService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private ExperimentRepository experimentRepository;

    @Autowired
    private FabRequestRepository requestRepository;

    @Autowired
    private WaferRepository waferRepository;

    @Autowired
    private WipTaskRepository wipTaskRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        wipTaskRepository.deleteAll();
        notificationRepository.deleteAll();
        waferRepository.deleteAll();
        requestRepository.deleteAll();
        experimentRepository.deleteAll();
        laboratoryRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity sysAdmin = new UserEntity("TS-0001", "ROLE_SYSADMIN", "System", "Admin", "IT Operations", "sysadmin@lims.local");
        UserEntity fabUser = new UserEntity("TS-1001", "ROLE_FAB_USER", "Fab", "User", "Factory Integration", "fab.user@lims.local");
        UserEntity manager = new UserEntity("TS-9001", "ROLE_LAB_MANAGER", "Lab", "Manager", "Laboratory Operations", "lab.manager@lims.local");
        userRepository.saveAll(List.of(sysAdmin, fabUser, manager));

        LaboratoryEntity labRa = laboratoryRepository.save(new LaboratoryEntity("LAB_RA", "Reliability Lab (RA)"));
        experimentRepository.save(new ExperimentEntity("exp_bake", labRa, "High-Temp Bake"));
        experimentRepository.save(new ExperimentEntity("exp_etest", labRa, "Electrical Test"));
    }

    @Test
    void fabUserCanCreateRequestAndManagerCanApproveIntoWipQueue() {
        FabRequestSummary created = service.createRequest(new CreateFabRequest(
                "TS-1001",
                "LAB_RA",
                List.of("exp_bake", "exp_etest"),
                List.of("W-1234", "W-5678"),
                "URGENT",
                "Please process before Friday."
        ));

        assertThat(created.status()).isEqualTo("PENDING");
        assertThat(created.priority()).isEqualTo("URGENT");
        assertThat(created.waferCount()).isEqualTo(2);
        assertThat(service.listNotifications("TS-0001")).hasSize(1);
        assertThat(service.listNotifications("TS-9001")).hasSize(1);
        assertThat(service.listNotifications("TS-1001")).hasSize(1);

        List<ManagerRequestSummary> pending = service.listPendingRequests();
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().id()).isEqualTo(created.id());
        assertThat(pending.getFirst().requester()).isEqualTo("Fab User");
        assertThat(pending.getFirst().waferIds()).containsExactly("W-1234", "W-5678");
        assertThat(pending.getFirst().experiments()).containsExactly("High-Temp Bake", "Electrical Test");

        service.approveRequest(created.id(), "TS-9001");

        assertThat(requestRepository.findById(created.id())).hasValueSatisfying(request ->
                assertThat(request.getStatus()).isEqualTo("APPROVED"));
        assertThat(wipTaskRepository.findAll()).hasSize(4);
        assertThat(service.listPendingWips()).hasSize(4);
        assertThat(service.listPendingRequests()).isEmpty();
        assertThat(service.listNotifications("TS-1001")).hasSize(2);

        service.markNotificationsRead("TS-1001");
        assertThat(service.listNotifications("TS-1001").getFirst().read()).isTrue();
        service.clearNotifications("TS-1001");
        assertThat(service.listNotifications("TS-1001")).isEmpty();
    }

    @Test
    void managerCanRejectPendingRequestWithReason() {
        FabRequestSummary created = service.createRequest(new CreateFabRequest(
                "TS-1001",
                "LAB_RA",
                List.of("exp_bake"),
                List.of("W-1111"),
                "NORMAL",
                "Missing fixture details."
        ));

        service.rejectRequest(created.id(), "TS-9001", "Capacity unavailable");

        assertThat(requestRepository.findById(created.id())).hasValueSatisfying(request -> {
            assertThat(request.getStatus()).isEqualTo("REJECTED");
            assertThat(request.getRejectReason()).isEqualTo("Capacity unavailable");
        });
        assertThat(wipTaskRepository.findAll()).isEmpty();
    }

    @Test
    void requesterDoesNotReceiveDuplicateRoleNotificationWhenSubmittingAsSystemAdmin() {
        FabRequestSummary created = service.createRequest(new CreateFabRequest(
                "TS-0001",
                "LAB_RA",
                List.of("exp_bake"),
                List.of("W-2222"),
                "NORMAL",
                "Admin submitted request."
        ));

        assertThat(created.status()).isEqualTo("PENDING");
        assertThat(service.listNotifications("TS-0001"))
                .hasSize(1)
                .first()
                .extracting("title")
                .isEqualTo("Request Submitted");
        assertThat(service.listNotifications("TS-9001")).hasSize(1);
    }
}
