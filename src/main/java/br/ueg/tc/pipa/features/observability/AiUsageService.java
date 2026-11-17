package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.features.observability.dto.AiUsageDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

@Service
public class AiUsageService {
    private static final ZoneId DASHBOARD_ZONE = ZoneId.of("America/Sao_Paulo");
    private final AiInferenceLogRepository repository;

    public AiUsageService(AiInferenceLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AiUsageDTO getUsage(LocalDateTime from, LocalDateTime to, String aiProvider) {
        return getUsage(from, to, aiProvider, null, null, null);
    }

    @Transactional(readOnly = true)
    public AiUsageDTO getUsage(LocalDateTime from, LocalDateTime to, String aiProvider,
                               String persona, String institution, String toolName) {
        String provider = aiProvider == null || aiProvider.isBlank() ? null : aiProvider.trim();
        if (provider != null && !provider.equals("local") && !provider.equals("external")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Origem de IA deve ser local ou external.");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período inválido.");
        }
        return repository.aggregateUsage(
                from == null ? null : from.atZone(DASHBOARD_ZONE).toInstant(),
                to == null ? null : to.atZone(DASHBOARD_ZONE).toInstant(), provider,
                normalizedFilter(persona), normalizedFilter(institution), normalizedFilter(toolName));
    }

    private String normalizedFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
