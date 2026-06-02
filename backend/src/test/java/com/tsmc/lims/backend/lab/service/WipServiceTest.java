package com.tsmc.lims.backend.lab.service;

import com.tsmc.lims.backend.lab.entity.WipTask;
import com.tsmc.lims.backend.lab.entity.enums.WipStatus;
import com.tsmc.lims.backend.lab.repository.WipTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WipServiceTest {

    private WipTaskRepository wipTaskRepository;
    private WipService service;

    @BeforeEach
    void setUp() {
        wipTaskRepository = mock(WipTaskRepository.class);
        service = new WipService(wipTaskRepository);
    }

    @Test
    void findQueue_returnsOnlyQueueStatusTasks() {
        WipTask task = new WipTask();
        task.setWaferCode("W-1001");
        task.setStatus(WipStatus.QUEUE);
        when(wipTaskRepository.findByStatus(WipStatus.QUEUE)).thenReturn(List.of(task));

        List<WipTask> result = service.findQueue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWaferCode()).isEqualTo("W-1001");
        assertThat(result.get(0).getStatus()).isEqualTo(WipStatus.QUEUE);
    }

    @Test
    void findQueue_returnsEmptyListWhenNoQueuedTasks() {
        when(wipTaskRepository.findByStatus(WipStatus.QUEUE)).thenReturn(List.of());

        assertThat(service.findQueue()).isEmpty();
    }

    @Test
    void findPendingSorting_returnsOnlyPendingSortingTasks() {
        WipTask task = new WipTask();
        task.setWaferCode("W-2001");
        task.setStatus(WipStatus.PENDING_SORTING);
        when(wipTaskRepository.findByStatus(WipStatus.PENDING_SORTING)).thenReturn(List.of(task));

        List<WipTask> result = service.findPendingSorting();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(WipStatus.PENDING_SORTING);
    }

    @Test
    void findAll_returnsAllTasksRegardlessOfStatus() {
        WipTask t1 = new WipTask();
        t1.setWaferCode("W-1001");
        t1.setStatus(WipStatus.QUEUE);
        WipTask t2 = new WipTask();
        t2.setWaferCode("W-1002");
        t2.setStatus(WipStatus.PROCESSING);
        WipTask t3 = new WipTask();
        t3.setWaferCode("W-1003");
        t3.setStatus(WipStatus.COMPLETED);
        when(wipTaskRepository.findAll()).thenReturn(List.of(t1, t2, t3));

        List<WipTask> result = service.findAll();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(WipTask::getStatus)
                .containsExactlyInAnyOrder(WipStatus.QUEUE, WipStatus.PROCESSING, WipStatus.COMPLETED);
    }
}
