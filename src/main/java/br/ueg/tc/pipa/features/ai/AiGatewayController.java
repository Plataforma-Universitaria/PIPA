package br.ueg.tc.pipa.features.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/v1")
public class AiGatewayController {
    private final AiGatewayService service;

    public AiGatewayController(AiGatewayService service) {
        this.service = service;
    }

    @PostMapping("/chat/completions")
    public ResponseEntity<JsonNode> complete(@RequestBody ObjectNode request) {
        AiGatewayService.GatewayResult result = service.complete(request);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-AI-Provider", result.provider());
        headers.add("X-AI-Model", result.model());
        headers.add("X-AI-Request-Id", result.requestId());
        return ResponseEntity.ok().headers(headers).body(result.body());
    }

    @ExceptionHandler(AiGatewayException.class)
    public ResponseEntity<Map<String, Object>> handleGatewayError(AiGatewayException exception) {
        return ResponseEntity.status(exception.getStatus()).body(Map.of("error", Map.of(
                "message", exception.getMessage(), "type", "ai_gateway_error")));
    }
}
