package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.features.observability.dto.AiUsageDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class AiUsageController {
    private final AiUsageService service;

    public AiUsageController(AiUsageService service) {
        this.service = service;
    }

    @GetMapping("/api/observability/ai-usage")
    public AiUsageDTO getUsage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String aiProvider,
            @RequestParam(required = false) String persona,
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) String toolName) {
        return service.getUsage(from, to, aiProvider, persona, institution, toolName);
    }
}
