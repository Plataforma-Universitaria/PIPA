package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.features.observability.dto.ObservabilityLogDTO;
import br.ueg.tc.pipa.features.observability.dto.PageResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller base para o dashboard de observabilidade (RFCP09).
 * Retorna logs de execução de ferramentas sem expor dados sensíveis.
 *
 * Autenticacao administrativa ainda nao foi definida. Este endpoint permanece
 * sem protecao especifica nesta fase do RFCP09.
 */
@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {

    private final ObservabilityService observabilityService;

    public ObservabilityController(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    /**
     * Consulta logs de execução com filtros opcionais.
     *
     * Todos os parâmetros presentes são aplicados simultaneamente.
     */
    @GetMapping("/logs")
    public ResponseEntity<PageResponseDTO<ObservabilityLogDTO>> getLogs(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Long userSessionId,
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) String persona,
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        ObservabilityFilter filter = new ObservabilityFilter(
                from, to, persona, toolName, institution, provider, channel,
                result, userSessionId, sessionId
        );
        return ResponseEntity.ok(PageResponseDTO.from(observabilityService.getLogs(filter, pageable)));
    }
}
