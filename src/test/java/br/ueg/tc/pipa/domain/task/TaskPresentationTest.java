package br.ueg.tc.pipa.domain.task;

import br.ueg.tc.pipa.infra.generics.GenericModel;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class TaskPresentationTest {
    @Test
    void distinguishesTheScheduledDateFromThePersistedCreationTime() {
        Task task = task();
        ReflectionTestUtils.setField(task, GenericModel.class, "createdAt",
                new org.joda.time.LocalDateTime(2026, 11, 15, 20, 30), org.joda.time.LocalDateTime.class);
        assertThat(task.toString()).contains("*Anotação do dia* _16/11/2026_",
                "Preparar apresentação", "Data de criação: 15/11/2026 20:30");
    }

    @Test
    void doesNotInventACreationDateForLegacyOrUnpersistedTasks() {
        assertThat(task().toString()).contains("Data de criação: não informada");
    }

    private Task task() {
        Task task = new Task();
        task.setDate(LocalDateTime.of(2026, 11, 16, 9, 0));
        task.setNote("Preparar apresentação");
        return task;
    }
}
