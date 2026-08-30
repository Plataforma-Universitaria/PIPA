package br.ueg.tc.pipa.features.observability.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrato agregado da visão geral. Somente dimensões operacionais são
 * expostas; nenhuma identidade de usuário ou sessão faz parte do payload.
 */
public record ObservabilityDashboardDTO(
        Summary summary,
        List<ToolUsage> topTools,
        List<DailyTrend> dailyTrend,
        List<Distribution> statusDistribution,
        List<Distribution> requestsByChannel
) {
    public record Summary(
            long totalRequests,
            Double requestVariationPercent,
            Double averageDurationMs,
            long activeTools,
            Double successRatePercent
    ) {
    }

    public record ToolUsage(String toolName, long requests) {
    }

    public record DailyTrend(LocalDate date, long requests) {
    }

    public record Distribution(String label, long requests, Double percentage) {
    }
}
