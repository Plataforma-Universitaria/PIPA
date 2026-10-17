package br.ueg.tc.pipa.features.guara;

import br.ueg.tc.pipa.features.dto.GuaraToolDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guara")
public class GuaraController {

    @Autowired
    private GuaraService guaraService;

    @GetMapping("/tools/{userExternalId}")
    public ResponseEntity<List<GuaraToolDTO>> listTools(@PathVariable String userExternalId,
                                                       @AuthenticationPrincipal Jwt jwt) {
        requireMatchingUser(jwt, userExternalId);
        return ResponseEntity.ok(guaraService.listTools(userExternalId));
    }

    @GetMapping("/tools/guest")
    public ResponseEntity<List<GuaraToolDTO>> listGuestTools() {
        return ResponseEntity.ok(guaraService.listGuestTools());
    }

    @PostMapping("/execute/{toolName}/{userExternalId}")
    public ResponseEntity<Object> executeTool(
            @PathVariable String toolName,
            @PathVariable String userExternalId,
            @RequestParam(required = false, defaultValue = "") String sessionId,
            @RequestParam(required = false, defaultValue = "TELEGRAM") String channel,
            @RequestBody(required = false) Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-AI-Turn-Id", required = false) String turnId) {

        requireMatchingUser(jwt, userExternalId);
        String parsedTurnId = null;
        if (turnId != null) {
            try {
                parsedTurnId = java.util.UUID.fromString(turnId).toString();
            } catch (IllegalArgumentException invalidTurnId) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-AI-Turn-Id deve ser um UUID válido");
            }
        }
        
        if (params == null) {
            params = Map.of();
        }
        
        try (MDC.MDCCloseable ignored = parsedTurnId == null
                ? null : MDC.putCloseable("aiTurnId", parsedTurnId)) {
            return ResponseEntity.ok(guaraService.executeTool(toolName, userExternalId, params, sessionId, channel));
        }
    }

    private void requireMatchingUser(Jwt jwt, String userExternalId) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT do usuário ausente");
        }
        if (!userExternalId.equals(jwt.getSubject())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "JWT não corresponde ao usuário solicitado");
        }
    }
}
