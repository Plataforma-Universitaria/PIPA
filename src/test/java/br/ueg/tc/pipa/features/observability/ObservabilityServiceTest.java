package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLog;
import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLogRepository;
import br.ueg.tc.pipa.domain.usersession.UserSessionRepository;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityLogDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityServiceTest {

    private UserSessionRepository userSessionRepository;

    @BeforeEach
    void setUp() {
        userSessionRepository = repositoryProxy(UserSessionRepository.class,
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    void shouldRedactDetailsBeforePersistingLog() {
        AtomicReference<ToolExecutionLog> savedLog = new AtomicReference<>();
        ToolExecutionLogRepository repository = repositoryProxy(
                ToolExecutionLogRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("save")) {
                        ToolExecutionLog log = (ToolExecutionLog) args[0];
                        savedLog.set(log);
                        return log;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        ObservabilityService service = createService(repository);

        service.logToolExecution(
                "consultar_notas",
                "1.0",
                "session-id",
                "Aluno",
                null,
                false,
                "Falha token=segredo cpf 12345678909"
        );

        assertThat(savedLog.get().getDetails())
                .doesNotContain("segredo", "12345678909")
                .contains("[REDACTED]");
    }

    @Test
    void shouldMapEntitiesToSafeDtos() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 24, 11, 0);
        ToolExecutionLog log = new ToolExecutionLog();
        log.setId(1L);
        log.setToolName("ajuda");
        log.setToolVersion("1.0");
        log.setPersona("Convidado");
        log.setResult("Sucesso");
        log.setTimestamp(timestamp);
        ToolExecutionLogRepository repository = repositoryProxy(
                ToolExecutionLogRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("findAll")) {
                        return List.of(log);
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        ObservabilityService service = createService(repository);

        List<ObservabilityLogDTO> result = service.getLogs(null, null, null, null);

        assertThat(result).containsExactly(new ObservabilityLogDTO(
                1L, "ajuda", "1.0", "Convidado", "Sucesso", timestamp));
    }

    private ObservabilityService createService(ToolExecutionLogRepository repository) {
        return new ObservabilityService(
                userSessionRepository,
                repository,
                new ObservabilityLogMapper(),
                new SensitiveDataRedactor()
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T repositoryProxy(Class<T> repositoryType,
                                  java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                repositoryType.getClassLoader(),
                new Class<?>[]{repositoryType},
                handler
        );
    }
}
