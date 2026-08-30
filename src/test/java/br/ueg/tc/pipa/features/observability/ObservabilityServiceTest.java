package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLog;
import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLogRepository;
import br.ueg.tc.pipa.domain.usersession.UserSessionRepository;
import br.ueg.tc.pipa.domain.user.UserRepository;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityLogDTO;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityFilterOptionsDTO;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityDashboardDTO;
import br.ueg.tc.pipa.domain.usersession.UserSession;
import br.ueg.tc.pipa_integrator.exceptions.institution.InstitutionCommunicationException;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureCategory;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.time.Duration;

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
                    if (method.getName().equals("findAll") && args != null && args.length == 2) {
                        return new PageImpl<>(List.of(log));
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        ObservabilityService service = createService(repository);

        Page<ObservabilityLogDTO> result = service.getLogs(
                new ObservabilityFilter(null, null, null, null, null,
                        null, null, null, null, null),
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).containsExactly(new ObservabilityLogDTO(
                1L, "ajuda", "1.0", "Convidado", "Sucesso", null,
                null, null, null, null, timestamp));
    }

    @Test
    void shouldNotPropagateObservabilityPersistenceFailure() {
        ToolExecutionLogRepository repository = repositoryProxy(
                ToolExecutionLogRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("save")) {
                        throw new IllegalStateException("banco indisponível");
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        ObservabilityService service = createService(repository);

        boolean persisted = service.logToolExecutionSafely(
                "consultar_notas", "1.0", "session-id", null,
                "Aluno", null, false, 10L,
                new IllegalStateException("falha original"),
                ProviderFailureStage.TOOL_INVOCATION
        );

        assertThat(persisted).isFalse();
    }

    @Test
    void shouldLimitPageSizeAndReplaceUnsupportedSort() {
        AtomicReference<Pageable> capturedPageable = new AtomicReference<>();
        ToolExecutionLogRepository repository = repositoryProxy(
                ToolExecutionLogRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("findAll") && args != null && args.length == 2) {
                        Pageable pageable = (Pageable) args[1];
                        capturedPageable.set(pageable);
                        return Page.empty(pageable);
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        ObservabilityService service = createService(repository);

        service.getLogs(null, PageRequest.of(2, 500, Sort.by("details")));

        assertThat(capturedPageable.get().getPageNumber()).isEqualTo(2);
        assertThat(capturedPageable.get().getPageSize()).isEqualTo(100);
        assertThat(capturedPageable.get().getSort().getOrderFor("timestamp"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
        assertThat(capturedPageable.get().getSort().getOrderFor("details")).isNull();
    }

    @Test
    void shouldReturnSortedCaseInsensitiveFilterOptions() {
        ToolExecutionLogRepository repository = repositoryProxy(
                ToolExecutionLogRepository.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "findDistinctPersonas" -> List.of(" Professor ", "aluno", "Aluno", " ");
                    case "findDistinctToolNames" -> List.of("notas", "ajuda");
                    case "findDistinctInstitutionNames" -> List.of("UEG");
                    case "findDistinctProviderPaths" -> List.of("ueg-provider");
                    case "findDistinctChannels" -> List.of("TELEGRAM");
                    case "findDistinctResults" -> List.of("Sucesso", "Falha");
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        ObservabilityFilterOptionsDTO options = createService(repository).getFilterOptions();

        assertThat(options.personas()).containsExactly("aluno", "Professor");
        assertThat(options.tools()).containsExactly("ajuda", "notas");
        assertThat(options.institutions()).containsExactly("UEG");
        assertThat(options.providers()).containsExactly("ueg-provider");
        assertThat(options.channels()).containsExactly("TELEGRAM");
        assertThat(options.results()).containsExactly("Falha", "Sucesso");
    }

    @Test
    void shouldAggregateDashboardAndCompareWithPreviousPeriod() {
        UserSession telegramSession = new UserSession();
        telegramSession.setChannel("TELEGRAM");

        ToolExecutionLog first = dashboardLog(
                "consultar_notas", "Sucesso", 100L,
                LocalDateTime.of(2026, 8, 29, 10, 0), telegramSession);
        ToolExecutionLog second = dashboardLog(
                "consultar_notas", "Falha", 300L,
                LocalDateTime.of(2026, 8, 29, 11, 0), telegramSession);
        ToolExecutionLog third = dashboardLog(
                "consultar_faltas", "sucesso", null,
                LocalDateTime.of(2026, 8, 30, 9, 0), null);

        ToolExecutionLogRepository repository = repositoryProxy(
                ToolExecutionLogRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("findAll") && args != null && args.length == 1) {
                        return List.of(first, second, third);
                    }
                    if (method.getName().equals("count") && args != null && args.length == 1) {
                        return 2L;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        ObservabilityFilter filter = new ObservabilityFilter(
                LocalDateTime.of(2026, 8, 29, 0, 0),
                LocalDateTime.of(2026, 8, 30, 23, 59),
                null, null, null, null, null, null, null, null);

        ObservabilityDashboardDTO dashboard = createService(repository).getDashboard(filter);

        assertThat(dashboard.summary()).isEqualTo(new ObservabilityDashboardDTO.Summary(
                3L, 50.0, 200.0, 2L, 66.67));
        assertThat(dashboard.topTools()).containsExactly(
                new ObservabilityDashboardDTO.ToolUsage("consultar_notas", 2L),
                new ObservabilityDashboardDTO.ToolUsage("consultar_faltas", 1L));
        assertThat(dashboard.dailyTrend()).containsExactly(
                new ObservabilityDashboardDTO.DailyTrend(java.time.LocalDate.of(2026, 8, 29), 2L),
                new ObservabilityDashboardDTO.DailyTrend(java.time.LocalDate.of(2026, 8, 30), 1L));
        assertThat(dashboard.statusDistribution())
                .extracting(ObservabilityDashboardDTO.Distribution::requests)
                .containsExactly(2L, 1L);
        assertThat(dashboard.requestsByChannel())
                .extracting(ObservabilityDashboardDTO.Distribution::label,
                        ObservabilityDashboardDTO.Distribution::requests)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("TELEGRAM", 2L),
                        org.assertj.core.groups.Tuple.tuple("Não informado", 1L));
    }

    @Test
    void shouldReturnEmptyDashboardWithoutUndefinedPercentages() {
        ToolExecutionLogRepository repository = repositoryProxy(
                ToolExecutionLogRepository.class,
                (proxy, method, args) -> {
                    if (method.getName().equals("findAll")) return List.of();
                    throw new UnsupportedOperationException(method.getName());
                });

        ObservabilityDashboardDTO dashboard = createService(repository).getDashboard(null);

        assertThat(dashboard.summary()).isEqualTo(new ObservabilityDashboardDTO.Summary(
                0L, null, null, 0L, null));
        assertThat(dashboard.topTools()).isEmpty();
        assertThat(dashboard.dailyTrend()).isEmpty();
        assertThat(dashboard.statusDistribution()).isEmpty();
        assertThat(dashboard.requestsByChannel()).isEmpty();
    }

    private ToolExecutionLog dashboardLog(String toolName, String result, Long duration,
                                           LocalDateTime timestamp, UserSession session) {
        ToolExecutionLog log = new ToolExecutionLog();
        log.setToolName(toolName);
        log.setResult(result);
        log.setDurationMs(duration);
        log.setTimestamp(timestamp);
        log.setUserSession(session);
        return log;
    }

    private ObservabilityService createService(ToolExecutionLogRepository repository) {
        UserRepository userRepository = repositoryProxy(UserRepository.class,
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                });
        ObservabilitySessionProperties properties = new ObservabilitySessionProperties();
        properties.setTtl(Duration.ofHours(1));
        return new ObservabilityService(
                userSessionRepository,
                repository,
                new ObservabilityLogMapper(),
                new SensitiveDataRedactor(),
                new ProviderFailureResolver(),
                userRepository,
                properties
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
