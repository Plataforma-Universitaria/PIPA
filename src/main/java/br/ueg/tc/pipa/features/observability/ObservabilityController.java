package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.features.observability.dto.ObservabilityLogDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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

    @Autowired
    private ObservabilityService observabilityService;

    /**
     * Consulta logs de execução com filtros opcionais.
     *
     * @param sessionId filtra por sessão (fingerprint/chat.id)
     * @param toolName  filtra por nome da ferramenta
     * @param from      início do intervalo de tempo (ISO datetime)
     * @param to        fim do intervalo de tempo (ISO datetime)
     */
    @GetMapping("/logs")
    public ResponseEntity<List<ObservabilityLogDTO>> getLogs(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<ObservabilityLogDTO> logs = observabilityService.getLogs(sessionId, toolName, from, to);
        return ResponseEntity.ok(logs);
    }
}
