package br.ueg.tc.pipa.features.observability.dto;

import java.time.LocalDateTime;

/**
 * Representacao segura de uma execucao para consulta de observabilidade.
 *
 * <p>Referencias ao usuario, fingerprint da sessao e detalhes internos da
 * execucao nao fazem parte do contrato HTTP.</p>
 */
public record ObservabilityLogDTO(
        Long id,
        String toolName,
        String toolVersion,
        String persona,
        String result,
        LocalDateTime timestamp
) {
}
