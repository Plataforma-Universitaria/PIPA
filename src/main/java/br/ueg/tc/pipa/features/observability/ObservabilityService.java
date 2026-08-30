package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLog;
import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLogRepository;
import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.user.UserRepository;
import br.ueg.tc.pipa.domain.usersession.UserSession;
import br.ueg.tc.pipa.domain.usersession.UserSessionRepository;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityLogDTO;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityFilterOptionsDTO;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityDashboardDTO;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ObservabilityService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "timestamp", "toolName", "persona", "result", "durationMs"
    );

    private final UserSessionRepository userSessionRepository;
    private final ToolExecutionLogRepository toolExecutionLogRepository;
    private final ObservabilityLogMapper observabilityLogMapper;
    private final SensitiveDataRedactor sensitiveDataRedactor;
    private final ProviderFailureResolver providerFailureResolver;
    private final UserRepository userRepository;
    private final ObservabilitySessionProperties sessionProperties;

    public ObservabilityService(UserSessionRepository userSessionRepository,
                                ToolExecutionLogRepository toolExecutionLogRepository,
                                ObservabilityLogMapper observabilityLogMapper,
                                SensitiveDataRedactor sensitiveDataRedactor,
                                ProviderFailureResolver providerFailureResolver,
                                UserRepository userRepository,
                                ObservabilitySessionProperties sessionProperties) {
        this.userSessionRepository = userSessionRepository;
        this.toolExecutionLogRepository = toolExecutionLogRepository;
        this.observabilityLogMapper = observabilityLogMapper;
        this.sensitiveDataRedactor = sensitiveDataRedactor;
        this.providerFailureResolver = providerFailureResolver;
        this.userRepository = userRepository;
        this.sessionProperties = sessionProperties;
    }

    /**
     * Obtém a sessão ativa do usuário e atualiza sua atividade. O lock no
     * usuário serializa criação/renovação concorrente inclusive entre nós da
     * aplicação que compartilham o mesmo banco.
     */
    @Transactional
    public UserSession startSession(User user, String fingerprint, String channel) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Usuário persistido é obrigatório para iniciar a sessão");
        }

        String normalizedFingerprint = normalizeFingerprint(fingerprint);
        String normalizedChannel = normalizeChannel(channel);
        User lockedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário da sessão não foi encontrado"));
        LocalDateTime now = LocalDateTime.now();
        List<UserSession> activeSessions = userSessionRepository.findActiveForUpdate(
                lockedUser.getId(), normalizedFingerprint, normalizedChannel);

        UserSession current = activeSessions.isEmpty() ? null : activeSessions.get(0);
        closeDuplicateActiveSessions(activeSessions, now);

        if (current != null) {
            LocalDateTime lastActivity = activityReference(current);
            LocalDateTime expiresAt = lastActivity.plus(sessionProperties.getTtl());
            if (expiresAt.isAfter(now)) {
                current.setLastActivityAt(now);
                return userSessionRepository.save(current);
            }
            current.setEndedAt(expiresAt);
            userSessionRepository.save(current);
        }

        return createSession(lockedUser, normalizedFingerprint, normalizedChannel, now);
    }

    /**
     * Encerra explicitamente as sessões ativas da identidade informada.
     */
    @Transactional
    public void endSession(User user, String fingerprint, String channel) {
        if (user == null || user.getId() == null) {
            return;
        }
        userRepository.findByIdForUpdate(user.getId()).ifPresent(lockedUser -> {
            List<UserSession> activeSessions = userSessionRepository.findActiveForUpdate(
                    lockedUser.getId(), normalizeFingerprint(fingerprint), normalizeChannel(channel));
            LocalDateTime now = LocalDateTime.now();
            activeSessions.forEach(session -> session.setEndedAt(now));
            userSessionRepository.saveAll(activeSessions);
        });
    }

    @Transactional
    public int closeInactiveSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minus(sessionProperties.getTtl());
        List<UserSession> inactiveSessions = userSessionRepository.findInactiveForUpdate(cutoff);
        for (UserSession session : inactiveSessions) {
            session.setEndedAt(activityReference(session).plus(sessionProperties.getTtl()));
        }
        userSessionRepository.saveAll(inactiveSessions);
        return inactiveSessions.size();
    }

    /**
     * Grava um registro de execução de ferramenta.
     * Chamado pelo GuaraService.executeTool() em bloco try/finally —
     * garante gravação tanto em sucesso quanto em falha.
     */
    public void logToolExecution(String toolName, String toolVersion, String sessionId,
                                 UserSession userSession, String persona, User user,
                                 boolean success, Long durationMs, Throwable failure,
                                 ProviderFailureStage fallbackStage) {
        ToolExecutionLog log = new ToolExecutionLog();
        log.setToolName(truncate(toolName == null || toolName.isBlank() ? "unknown_tool" : toolName, 100));
        log.setToolVersion(truncate(toolVersion, 20));
        log.setSessionId(truncate(sessionId, 100));
        log.setUserSession(userSession);
        log.setPersona(truncate(persona, 50));
        log.setUser(user);
        log.setResult(success ? "Sucesso" : "Falha");
        log.setDurationMs(durationMs);
        if (!success) {
            ResolvedProviderFailure resolvedFailure = providerFailureResolver.resolve(failure, fallbackStage);
            log.setFailureCode(resolvedFailure.code());
            log.setFailureCategory(resolvedFailure.category());
            log.setFailureStage(resolvedFailure.stage());
            log.setRetryable(resolvedFailure.retryable());
            log.setDetails(truncate(sensitiveDataRedactor.redact(resolvedFailure.safeDetails()), 1000));
        }
        log.setTimestamp(LocalDateTime.now());
        toolExecutionLogRepository.save(log);
    }

    /**
     * A observabilidade é best effort: indisponibilidade do repositório de logs
     * não pode alterar o retorno nem substituir a falha original da ferramenta.
     */
    public boolean logToolExecutionSafely(String toolName, String toolVersion, String sessionId,
                                           UserSession userSession, String persona, User user,
                                           boolean success, Long durationMs, Throwable failure,
                                           ProviderFailureStage fallbackStage) {
        try {
            logToolExecution(toolName, toolVersion, sessionId, userSession, persona, user,
                    success, durationMs, failure, fallbackStage);
            return true;
        } catch (RuntimeException observabilityFailure) {
            log.error("Falha ao persistir observabilidade da ferramenta. toolName={}, stage={}",
                    safeToolName(toolName), fallbackStage, observabilityFailure);
            return false;
        }
    }

    /** Consulta paginada na qual todos os filtros informados são acumulados. */
    @Transactional(readOnly = true)
    public Page<ObservabilityLogDTO> getLogs(ObservabilityFilter filter, Pageable pageable) {
        Pageable normalizedPageable = normalizePageable(pageable);
        return toolExecutionLogRepository
                .findAll(ToolExecutionLogSpecification.from(filter), normalizedPageable)
                .map(observabilityLogMapper::toDTO);
    }

    /** Retorna somente dimensões observáveis, sem identidade ou referência de sessão. */
    @Transactional(readOnly = true)
    public ObservabilityFilterOptionsDTO getFilterOptions() {
        return new ObservabilityFilterOptionsDTO(
                cleanDistinctOptions(toolExecutionLogRepository.findDistinctPersonas()),
                cleanDistinctOptions(toolExecutionLogRepository.findDistinctToolNames()),
                cleanDistinctOptions(toolExecutionLogRepository.findDistinctInstitutionNames()),
                cleanDistinctOptions(toolExecutionLogRepository.findDistinctProviderPaths()),
                cleanDistinctOptions(toolExecutionLogRepository.findDistinctChannels()),
                cleanDistinctOptions(toolExecutionLogRepository.findDistinctResults())
        );
    }

    /**
     * Agrega a visão geral a partir do mesmo conjunto filtrado usado pela
     * listagem. A comparação temporal só existe quando ambos os limites foram
     * informados e o período anterior contém registros.
     */
    @Transactional(readOnly = true)
    public ObservabilityDashboardDTO getDashboard(ObservabilityFilter filter) {
        List<ToolExecutionLog> logs = toolExecutionLogRepository.findAll(
                ToolExecutionLogSpecification.from(filter));
        long total = logs.size();
        long successful = logs.stream().filter(this::isSuccessful).count();

        Double averageDuration = rounded(logs.stream()
                .map(ToolExecutionLog::getDurationMs)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null));

        long activeTools = logs.stream()
                .map(ToolExecutionLog::getToolName)
                .filter(this::hasText)
                .map(this::normalizedKey)
                .distinct()
                .count();

        return new ObservabilityDashboardDTO(
                new ObservabilityDashboardDTO.Summary(
                        total,
                        calculateVariation(filter, total),
                        averageDuration,
                        activeTools,
                        percentage(successful, total)
                ),
                aggregateTopTools(logs),
                aggregateDailyTrend(logs),
                aggregateDistribution(logs, ToolExecutionLog::getResult, "Não informado"),
                aggregateDistribution(logs,
                        log -> log.getUserSession() == null ? null : log.getUserSession().getChannel(),
                        "Não informado")
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private UserSession createSession(User user, String fingerprint, String channel, LocalDateTime now) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setFingerprint(fingerprint);
        session.setChannel(channel);
        session.setStartedAt(now);
        session.setLastActivityAt(now);
        return userSessionRepository.save(session);
    }

    private void closeDuplicateActiveSessions(List<UserSession> activeSessions, LocalDateTime now) {
        for (int index = 1; index < activeSessions.size(); index++) {
            UserSession duplicate = activeSessions.get(index);
            duplicate.setEndedAt(now);
            userSessionRepository.save(duplicate);
        }
    }

    private LocalDateTime activityReference(UserSession session) {
        return session.getLastActivityAt() != null
                ? session.getLastActivityAt()
                : session.getStartedAt();
    }

    private String normalizeFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            throw new IllegalArgumentException("Fingerprint da sessão é obrigatório");
        }
        return truncate(fingerprint.trim(), 100);
    }

    private String normalizeChannel(String channel) {
        String value = channel == null || channel.isBlank() ? "TELEGRAM" : channel.trim();
        return truncate(value.toUpperCase(Locale.ROOT), 50);
    }

    private String safeToolName(String toolName) {
        return truncate(toolName == null ? "unknown_tool" : toolName.replaceAll("[^a-zA-Z0-9_-]", "_"), 100);
    }

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp"));
        }

        List<Sort.Order> acceptedOrders = pageable.getSort().stream()
                .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
                .toList();
        Sort sort = acceptedOrders.isEmpty()
                ? Sort.by(Sort.Direction.DESC, "timestamp")
                : Sort.by(acceptedOrders);
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(pageable.getPageNumber(), 0), pageSize, sort);
    }

    private List<String> cleanDistinctOptions(List<String> values) {
        Map<String, String> uniqueValues = new TreeMap<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    String trimmed = value.trim();
                    uniqueValues.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
                }
            }
        }
        return List.copyOf(uniqueValues.values());
    }

    private Double calculateVariation(ObservabilityFilter filter, long currentTotal) {
        if (filter == null || filter.from() == null || filter.to() == null
                || !filter.to().isAfter(filter.from())) {
            return null;
        }
        Duration duration = Duration.between(filter.from(), filter.to());
        LocalDateTime previousTo = filter.from().minusNanos(1);
        LocalDateTime previousFrom = previousTo.minus(duration);
        ObservabilityFilter previousFilter = new ObservabilityFilter(
                previousFrom, previousTo, filter.persona(), filter.toolName(),
                filter.institution(), filter.provider(), filter.channel(), filter.result(),
                filter.userSessionId(), filter.sessionId()
        );
        long previousTotal = toolExecutionLogRepository.count(
                ToolExecutionLogSpecification.from(previousFilter));
        if (previousTotal == 0) {
            return null;
        }
        return rounded(((currentTotal - previousTotal) * 100.0) / previousTotal);
    }

    private List<ObservabilityDashboardDTO.ToolUsage> aggregateTopTools(List<ToolExecutionLog> logs) {
        return aggregateCounts(logs, ToolExecutionLog::getToolName, "unknown_tool").entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                .limit(10)
                .map(entry -> new ObservabilityDashboardDTO.ToolUsage(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<ObservabilityDashboardDTO.DailyTrend> aggregateDailyTrend(List<ToolExecutionLog> logs) {
        return logs.stream()
                .filter(log -> log.getTimestamp() != null)
                .collect(Collectors.groupingBy(
                        log -> log.getTimestamp().toLocalDate(),
                        TreeMap::new,
                        Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new ObservabilityDashboardDTO.DailyTrend(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<ObservabilityDashboardDTO.Distribution> aggregateDistribution(
            List<ToolExecutionLog> logs,
            Function<ToolExecutionLog, String> dimension,
            String missingLabel) {
        long total = logs.size();
        return aggregateCounts(logs, dimension, missingLabel).entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                .map(entry -> new ObservabilityDashboardDTO.Distribution(
                        entry.getKey(), entry.getValue(), percentage(entry.getValue(), total)))
                .toList();
    }

    private Map<String, Long> aggregateCounts(List<ToolExecutionLog> logs,
                                               Function<ToolExecutionLog, String> dimension,
                                               String missingLabel) {
        Map<String, String> labels = new TreeMap<>();
        for (ToolExecutionLog log : logs) {
            String rawValue = dimension.apply(log);
            String label = hasText(rawValue) ? rawValue.trim() : missingLabel;
            labels.putIfAbsent(normalizedKey(label), label);
        }
        Map<String, Long> counts = logs.stream().collect(Collectors.groupingBy(
                log -> {
                    String rawValue = dimension.apply(log);
                    String label = hasText(rawValue) ? rawValue.trim() : missingLabel;
                    return normalizedKey(label);
                },
                Collectors.counting()));
        Map<String, Long> result = new LinkedHashMap<>();
        labels.forEach((key, label) -> result.put(label, counts.getOrDefault(key, 0L)));
        return result;
    }

    private boolean isSuccessful(ToolExecutionLog log) {
        return hasText(log.getResult()) && "sucesso".equals(normalizedKey(log.getResult()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizedKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private Double percentage(long part, long total) {
        return total == 0 ? null : rounded((part * 100.0) / total);
    }

    private Double rounded(Double value) {
        return value == null ? null : Math.round(value * 100.0) / 100.0;
    }
}
