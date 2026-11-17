package br.ueg.tc.pipa.features.observability.dto;

/** Somas conhecidas e cobertura independente de cada contagem; ausência não é zero. */
public record AiUsageDTO(long totalInferences, long inferencesWithTokenUsage, Long totalTokens,
                         Long inputTokens, Long outputTokens,
                         long inferencesWithInputTokens, long inferencesWithOutputTokens) {
}
