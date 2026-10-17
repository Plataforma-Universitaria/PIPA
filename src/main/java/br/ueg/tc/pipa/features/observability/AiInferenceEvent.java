package br.ueg.tc.pipa.features.observability;

/** Campos permitidos na telemetria; conteúdo, parâmetros e credenciais não fazem parte do contrato. */
public record AiInferenceEvent(
        String requestId,
        String turnId,
        String operation,
        String routingMode,
        String routingReason,
        String provider,
        String model,
        Long durationMs,
        Long inputTokens,
        Long outputTokens,
        Long totalTokens,
        String result,
        String failureCode
) {}
