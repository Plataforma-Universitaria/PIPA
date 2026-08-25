package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLog;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityLogDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityLogMapperTest {

    private final ObservabilityLogMapper mapper = new ObservabilityLogMapper();

    @Test
    void shouldExposeOnlySafeObservabilityFields() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 24, 10, 30);
        ToolExecutionLog log = new ToolExecutionLog();
        log.setId(15L);
        log.setToolName("consultar_notas");
        log.setToolVersion("1.0");
        log.setPersona("Aluno");
        log.setResult("Sucesso");
        log.setTimestamp(timestamp);
        log.setSessionId("telegram-chat-id");
        log.setDetails("conteudo interno");

        ObservabilityLogDTO dto = mapper.toDTO(log);

        assertThat(dto).isEqualTo(new ObservabilityLogDTO(
                15L, "consultar_notas", "1.0", "Aluno", "Sucesso", timestamp));
        assertThat(Arrays.stream(ObservabilityLogDTO.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("user", "sessionId", "details");
    }
}
