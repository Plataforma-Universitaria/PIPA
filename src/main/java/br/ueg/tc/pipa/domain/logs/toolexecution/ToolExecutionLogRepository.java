package br.ueg.tc.pipa.domain.logs.toolexecution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ToolExecutionLogRepository extends JpaRepository<ToolExecutionLog, Long> {
    List<ToolExecutionLog> findBySessionId(String sessionId);
    List<ToolExecutionLog> findByUserSessionId(Long userSessionId);
    List<ToolExecutionLog> findByUserId(Long userId);
    List<ToolExecutionLog> findByToolName(String toolName);
    List<ToolExecutionLog> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
    List<ToolExecutionLog> findByToolNameAndTimestampBetween(String toolName, LocalDateTime from, LocalDateTime to);
}
