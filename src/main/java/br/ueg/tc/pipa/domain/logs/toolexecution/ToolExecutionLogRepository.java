package br.ueg.tc.pipa.domain.logs.toolexecution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ToolExecutionLogRepository extends JpaRepository<ToolExecutionLog, Long>,
        JpaSpecificationExecutor<ToolExecutionLog> {
    List<ToolExecutionLog> findBySessionId(String sessionId);
    List<ToolExecutionLog> findByUserSessionId(Long userSessionId);
    List<ToolExecutionLog> findByUserId(Long userId);
    List<ToolExecutionLog> findByToolName(String toolName);
    List<ToolExecutionLog> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
    List<ToolExecutionLog> findByToolNameAndTimestampBetween(String toolName, LocalDateTime from, LocalDateTime to);

    @Query("select distinct log.persona from ToolExecutionLog log where log.persona is not null")
    List<String> findDistinctPersonas();

    @Query("select distinct log.toolName from ToolExecutionLog log where log.toolName is not null")
    List<String> findDistinctToolNames();

    @Query("""
            select distinct institution.shortName
              from ToolExecutionLog log
              join log.user logUser
              join logUser.institution institution
             where institution.shortName is not null
            """)
    List<String> findDistinctInstitutionNames();

    @Query("""
            select distinct institution.providerPath
              from ToolExecutionLog log
              join log.user logUser
              join logUser.institution institution
             where institution.providerPath is not null
            """)
    List<String> findDistinctProviderPaths();

    @Query("""
            select distinct session.channel
              from ToolExecutionLog log
              join log.userSession session
             where session.channel is not null
            """)
    List<String> findDistinctChannels();

    @Query("select distinct log.result from ToolExecutionLog log where log.result is not null")
    List<String> findDistinctResults();
}
