package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLog;
import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObservabilityExportServiceTest {

    @Test
    void shouldGenerateSafeExcelFriendlyCsvFromFilteredLogs() {
        ToolExecutionLog log = logWithInternalSecret();
        ToolExecutionLogRepository repository = repositoryReturning(log);
        ObservabilityExportService service = new ObservabilityExportService(
                repository, new ObservabilityLogMapper());

        ObservabilityExportService.ExportedFile file = service.export(filter(), "CSV");
        String csv = new String(file.content(), StandardCharsets.UTF_8);

        assertThat(file.mediaType()).isEqualTo("text/csv");
        assertThat(file.extension()).isEqualTo("csv");
        assertThat(csv).startsWith("\uFEFFid,toolName");
        assertThat(csv).contains("consultar_notas", "INSTITUTION_COMMUNICATION_ERROR");
        assertThat(csv).doesNotContain("segredo-interno", "session-private", "fingerprint");
    }

    @Test
    void shouldNeutralizeSpreadsheetFormulaCells() {
        ToolExecutionLog log = logWithInternalSecret();
        log.setPersona("=HYPERLINK(\"https://example.invalid\")");
        ObservabilityExportService service = new ObservabilityExportService(
                repositoryReturning(log), new ObservabilityLogMapper());

        String csv = new String(service.export(filter(), "csv").content(), StandardCharsets.UTF_8);

        assertThat(csv).contains("\"'=HYPERLINK(\"\"https://example.invalid\"\")\"");
    }

    @Test
    void shouldGeneratePdfWithAValidHeader() {
        ObservabilityExportService service = new ObservabilityExportService(
                repositoryReturning(logWithInternalSecret()), new ObservabilityLogMapper());

        ObservabilityExportService.ExportedFile file = service.export(filter(), "pdf");

        assertThat(file.mediaType()).isEqualTo("application/pdf");
        assertThat(file.extension()).isEqualTo("pdf");
        assertThat(file.content()).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        assertThat(file.content().length).isGreaterThan(500);
    }

    @Test
    void shouldRejectUnsupportedFormat() {
        ObservabilityExportService service = new ObservabilityExportService(
                repositoryReturning(logWithInternalSecret()), new ObservabilityLogMapper());

        assertThatThrownBy(() -> service.export(filter(), "xlsx"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    private ToolExecutionLog logWithInternalSecret() {
        ToolExecutionLog log = new ToolExecutionLog();
        log.setId(7L);
        log.setToolName("consultar_notas");
        log.setToolVersion("1.0");
        log.setPersona("Aluno");
        log.setResult("Falha");
        log.setDurationMs(120L);
        log.setFailureCode("INSTITUTION_COMMUNICATION_ERROR");
        log.setTimestamp(LocalDateTime.of(2026, 8, 30, 10, 0));
        log.setDetails("segredo-interno");
        log.setSessionId("session-private");
        return log;
    }

    private ObservabilityFilter filter() {
        return new ObservabilityFilter(null, null, "Aluno", null, null,
                null, null, null, null, null);
    }

    @SuppressWarnings("unchecked")
    private ToolExecutionLogRepository repositoryReturning(ToolExecutionLog log) {
        return (ToolExecutionLogRepository) Proxy.newProxyInstance(
                ToolExecutionLogRepository.class.getClassLoader(),
                new Class<?>[]{ToolExecutionLogRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findAll") && args != null
                            && args.length == 2 && args[1] instanceof Sort) {
                        return List.of(log);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
