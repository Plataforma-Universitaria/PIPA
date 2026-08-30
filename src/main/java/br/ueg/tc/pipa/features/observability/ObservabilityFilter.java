package br.ueg.tc.pipa.features.observability;

import java.time.LocalDateTime;

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
        String sessionId
) {
}
