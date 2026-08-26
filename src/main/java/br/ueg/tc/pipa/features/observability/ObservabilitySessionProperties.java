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

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("observability.session.ttl deve ser maior que zero");
        }
        this.ttl = ttl;
    }
}
