package br.ueg.tc.pipa.features.observability;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ObservabilitySessionScheduler {

    private final ObservabilityService observabilityService;

    public ObservabilitySessionScheduler(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    @Scheduled(fixedDelayString = "${observability.session.cleanup-interval:60s}")
    public void closeInactiveSessions() {
        observabilityService.closeInactiveSessions();
    }
}
