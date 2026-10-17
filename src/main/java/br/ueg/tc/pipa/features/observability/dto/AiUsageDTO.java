package br.ueg.tc.pipa.features.observability.dto;

/** Total conhecido de tokens; valores ausentes não são convertidos em zero. */
public record AiUsageDTO(long totalInferences, long inferencesWithTokenUsage, Long totalTokens) {
}
