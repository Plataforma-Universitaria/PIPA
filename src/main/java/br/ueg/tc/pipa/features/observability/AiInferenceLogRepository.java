package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.features.observability.dto.AiUsageDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AiInferenceLogRepository extends JpaRepository<AiInferenceLog, Long> {
    @Query("""
            select new br.ueg.tc.pipa.features.observability.dto.AiUsageDTO(
                count(log), count(log.totalTokens), sum(log.totalTokens),
                sum(log.inputTokens), sum(log.outputTokens),
                count(log.inputTokens), count(log.outputTokens))
            from AiInferenceLog log
            where (cast(:from as timestamp) is null or log.timestamp >= :from)
              and (cast(:to as timestamp) is null or log.timestamp <= :to)
              and (:provider is null or coalesce(nullif(trim(log.provider), ''), 'external') = :provider)
              and ((:persona is null and :institution is null and :toolName is null) or exists (
                  select 1 from ToolExecutionLog tool
                  left join tool.user toolUser
                  left join toolUser.institution institution
                  where tool.turnId = log.turnId
                    and (:persona is null or lower(tool.persona) = :persona)
                    and (:institution is null or lower(institution.shortName) = :institution)
                    and (:toolName is null or lower(tool.toolName) = :toolName)
              ))
            """)
    AiUsageDTO aggregateUsage(@Param("from") Instant from, @Param("to") Instant to,
                              @Param("provider") String provider,
                              @Param("persona") String persona,
                              @Param("institution") String institution,
                              @Param("toolName") String toolName);

    default AiUsageDTO aggregateUsage(Instant from, Instant to, String provider) {
        return aggregateUsage(from, to, provider, null, null, null);
    }

    @Modifying
    @Query("delete from AiInferenceLog log where log.timestamp < :cutoff")
    int deleteByTimestampBefore(@Param("cutoff") Instant cutoff);
}
