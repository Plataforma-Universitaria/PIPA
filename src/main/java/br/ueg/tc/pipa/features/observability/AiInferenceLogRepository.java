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
                count(log), count(log.totalTokens), sum(log.totalTokens))
            from AiInferenceLog log
            where (cast(:from as timestamp) is null or log.timestamp >= :from)
              and (cast(:to as timestamp) is null or log.timestamp <= :to)
              and (:provider is null or coalesce(nullif(trim(log.provider), ''), 'external') = :provider)
            """)
    AiUsageDTO aggregateUsage(@Param("from") Instant from, @Param("to") Instant to,
                              @Param("provider") String provider);

    @Modifying
    @Query("delete from AiInferenceLog log where log.timestamp < :cutoff")
    int deleteByTimestampBefore(@Param("cutoff") Instant cutoff);
}
