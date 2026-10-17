package br.ueg.tc.pipa.features.observability;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Filtros opcionais compartilhados pelas consultas de observabilidade.
 * Identificadores de sessão são aceitos apenas para diagnóstico e nunca são
 * incluídos no DTO retornado.
 */
public record ObservabilityFilter(
        LocalDateTime from,
        LocalDateTime to,
        String persona,
        String toolName,
        String institution,
        String provider,
        String channel,
        String result,
        Long userSessionId,
        String sessionId,
        String aiProvider
) {
    public ObservabilityFilter {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data final deve ser igual ou posterior à data inicial.");
        }
        aiProvider = aiProvider == null || aiProvider.isBlank() ? null : aiProvider.trim().toLowerCase(java.util.Locale.ROOT);
        if (aiProvider != null && !aiProvider.equals("local") && !aiProvider.equals("external")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Origem do modelo deve ser local ou external.");
        }
    }

    public ObservabilityFilter(LocalDateTime from, LocalDateTime to, String persona, String toolName,
                               String institution, String provider, String channel, String result,
                               Long userSessionId, String sessionId) {
        this(from, to, persona, toolName, institution, provider, channel, result, userSessionId, sessionId, null);
    }
}
