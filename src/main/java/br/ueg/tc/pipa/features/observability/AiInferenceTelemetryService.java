package br.ueg.tc.pipa.features.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class AiInferenceTelemetryService {
    private static final Logger log = LoggerFactory.getLogger(AiInferenceTelemetryService.class);
    private final AiInferenceLogRepository repository;
    private final Duration retention;

    public AiInferenceTelemetryService(AiInferenceLogRepository repository,
                                       @Value("${ai.gateway.telemetry.retention:90d}") Duration retention) {
        this.repository = repository;
        this.retention = retention;
        if (retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("Retenção de inferências deve ser positiva");
        }
    }

    /** Uma falha na observabilidade não altera o resultado da inferência. */
    public void recordSafely(AiInferenceEvent event) {
        try {
            repository.save(new AiInferenceLog(event));
        } catch (RuntimeException failure) {
            log.warn("Não foi possível persistir metadados de inferência: requestId={} errorType={}",
                    event.requestId(), failure.getClass().getSimpleName());
        }
    }

    @Scheduled(fixedDelayString = "${ai.gateway.telemetry.cleanup-interval:24h}")
    @Transactional
    public void deleteExpired() {
        repository.deleteByTimestampBefore(Instant.now().minus(retention));
    }
}
