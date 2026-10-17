package br.ueg.tc.pipa.features.observability.dto;

import java.util.List;
import java.util.Map;

/** Valores seguros disponíveis para montar os controles de filtro. */
public record ObservabilityFilterOptionsDTO(
        List<String> personas,
        List<String> tools,
        List<String> institutions,
        List<String> providers,
        List<String> channels,
        List<String> results,
        Map<String, String> toolLabels
) {
}
