package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.features.observability.dto.ObservabilityLogDTO;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityFilterOptionsDTO;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityDashboardDTO;
import br.ueg.tc.pipa.features.observability.dto.PageResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalDate;

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
    private final ObservabilityExportService observabilityExportService;

    public ObservabilityController(ObservabilityService observabilityService,
                                   ObservabilityExportService observabilityExportService) {
        this.observabilityService = observabilityService;
        this.observabilityExportService = observabilityExportService;
    }

    @GetMapping("/filters")
    public ResponseEntity<ObservabilityFilterOptionsDTO> getFilterOptions() {
        return ResponseEntity.ok(observabilityService.getFilterOptions());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ObservabilityDashboardDTO> getDashboard(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Long userSessionId,
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) String persona,
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(observabilityService.getDashboard(buildFilter(
                sessionId, userSessionId, toolName, persona, institution,
                provider, channel, result, from, to)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Long userSessionId,
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) String persona,
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        ObservabilityExportService.ExportedFile file = observabilityExportService.export(
                buildFilter(sessionId, userSessionId, toolName, persona, institution,
                        provider, channel, result, from, to),
                format);
        String filename = "observabilidade-" + LocalDate.now() + "." + file.extension();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(file.content());
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

        ObservabilityFilter filter = buildFilter(sessionId, userSessionId, toolName, persona,
                institution, provider, channel, result, from, to);
        return ResponseEntity.ok(PageResponseDTO.from(observabilityService.getLogs(filter, pageable)));
    }

    private ObservabilityFilter buildFilter(String sessionId, Long userSessionId,
                                             String toolName, String persona,
                                             String institution, String provider,
                                             String channel, String result,
                                             LocalDateTime from, LocalDateTime to) {
        return new ObservabilityFilter(from, to, persona, toolName, institution,
                provider, channel, result, userSessionId, sessionId);
    }
}
