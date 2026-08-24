package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLog;
import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLogRepository;
import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.usersession.UserSession;
import br.ueg.tc.pipa.domain.usersession.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ObservabilityService {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private ToolExecutionLogRepository toolExecutionLogRepository;

    /**
     * Garante que existe um registro de sessão no Postgres para este fingerprint.
     * Se já existir, retorna o existente sem criar duplicata.
     * ended_at permanece null — será preenchido futuramente.
     */
    public UserSession startSession(User user, String fingerprint, String channel) {
        return userSessionRepository.findByFingerprint(fingerprint).orElseGet(() -> {
            UserSession session = new UserSession();
            session.setUser(user);
            session.setFingerprint(fingerprint);
            session.setChannel(channel != null ? channel : "TELEGRAM");
            session.setStartedAt(LocalDateTime.now());
            return userSessionRepository.save(session);
        });
    }

    /**
     * Encerra uma sessão registrando o ended_at.
     * Reservado para implementação futura.
     */
    public void endSession(String fingerprint) {
        userSessionRepository.findByFingerprint(fingerprint).ifPresent(session -> {
            if (session.getEndedAt() == null) {
                session.setEndedAt(LocalDateTime.now());
                userSessionRepository.save(session);
            }
        });
    }

    /**
     * Grava um registro de execução de ferramenta.
     * Chamado pelo GuaraService.executeTool() em bloco try/finally —
     * garante gravação tanto em sucesso quanto em falha.
     */
    public void logToolExecution(String toolName, String toolVersion, String sessionId,
                                  String persona, User user, boolean success, String details) {
        ToolExecutionLog log = new ToolExecutionLog();
        log.setToolName(toolName);
        log.setToolVersion(toolVersion);
        log.setSessionId(sessionId);
        log.setPersona(persona);
        log.setUser(user);
        log.setResult(success ? "Sucesso" : "Falha");
        log.setDetails(truncate(details, 1000));
        log.setTimestamp(LocalDateTime.now());
        toolExecutionLogRepository.save(log);
    }

    /**
     * Retorna todos os logs, com filtros opcionais.
     */
    public List<ToolExecutionLog> getLogs(String sessionId, String toolName,
                                           LocalDateTime from, LocalDateTime to) {
        if (sessionId != null && !sessionId.isBlank()) {
            return toolExecutionLogRepository.findBySessionId(sessionId);
        }
        if (toolName != null && !toolName.isBlank() && from != null && to != null) {
            return toolExecutionLogRepository.findByToolNameAndTimestampBetween(toolName, from, to);
        }
        if (toolName != null && !toolName.isBlank()) {
            return toolExecutionLogRepository.findByToolName(toolName);
        }
        if (from != null && to != null) {
            return toolExecutionLogRepository.findByTimestampBetween(from, to);
        }
        return toolExecutionLogRepository.findAll();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
