package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLog;
import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLogRepository;
import br.ueg.tc.pipa.domain.usersession.UserSessionRepository;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityLogDTO;
import br.ueg.tc.pipa_integrator.exceptions.institution.InstitutionCommunicationException;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureCategory;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureStage;
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
    void shouldPersistNormalizedFailureWithoutSensitiveMessage() {
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
                null,
                "Aluno",
                null,
                false,
                null,
                new InstitutionCommunicationException(
                        "Falha token=segredo cpf 12345678909",
                        new IllegalStateException("corpo sensível")
                ),
                ProviderFailureStage.TOOL_INVOCATION
        );

        assertThat(savedLog.get().getDetails())
                .isEqualTo("InstitutionCommunicationException");
        assertThat(savedLog.get().getFailureCode()).isEqualTo("INSTITUTION_COMMUNICATION_ERROR");
        assertThat(savedLog.get().getFailureCategory()).isEqualTo(ProviderFailureCategory.COMMUNICATION);
        assertThat(savedLog.get().getFailureStage()).isEqualTo(ProviderFailureStage.PROVIDER_CALL);
        assertThat(savedLog.get().getRetryable()).isTrue();
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
                1L, "ajuda", "1.0", "Convidado", "Sucesso", null,
                null, null, null, null, timestamp));
    }

    private ObservabilityService createService(ToolExecutionLogRepository repository) {
        return new ObservabilityService(
                userSessionRepository,
                repository,
                new ObservabilityLogMapper(),
                new SensitiveDataRedactor(),
                new ProviderFailureResolver()
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
