package br.ueg.tc.pipa.features.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuração do ciclo de vida das sessões históricas de observabilidade.
 */
@Component
@ConfigurationProperties(prefix = "observability.session")
public class ObservabilitySessionProperties {

    private Duration ttl = Duration.ofHours(1);
    private Duration cleanupInterval = Duration.ofMinutes(1);

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("observability.session.ttl deve ser maior que zero");
        }
        this.ttl = ttl;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        if (cleanupInterval == null || cleanupInterval.isZero() || cleanupInterval.isNegative()) {
            throw new IllegalArgumentException("observability.session.cleanup-interval deve ser maior que zero");
        }
        this.cleanupInterval = cleanupInterval;
    }
}
