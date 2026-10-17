package br.ueg.tc.pipa.config;

import br.ueg.tc.pipa_integrator.exceptions.BusinessException;
import br.ueg.tc.pipa_integrator.exceptions.user.UserNotFoundException;
import br.ueg.tc.pipa.features.ai.AiGatewayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        log.warn("Sessão expirada ou usuário não encontrado: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Sessão expirada");
        body.put("message", "Sua sessão expirou. Por favor, faça login novamente.");
        body.put("code", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        Throwable cause = ex;
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (cause != null && visited.add(cause)) {
            if (cause instanceof AiGatewayException aiFailure) return handleAiGateway(aiFailure);
            cause = cause.getCause();
        }
        log.warn("Erro de negócio: {} (code: {})", ex.getMessage(), ex.getCode());

        HttpStatus httpStatus = resolveHttpStatus(ex.getCode());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", httpStatus.value());
        body.put("error", httpStatus.getReasonPhrase());
        body.put("message", ex.getMessage());
        body.put("code", ex.getMessage());

        return ResponseEntity.status(httpStatus).body(body);
    }

    @ExceptionHandler(AiGatewayException.class)
    public ResponseEntity<Map<String, Object>> handleAiGateway(AiGatewayException ex) {
        log.warn("Falha na inferência de IA: {}", ex.errorCode());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", ex.getStatus().value());
        body.put("error", ex.getStatus().getReasonPhrase());
        body.put("code", ex.errorCode());
        body.put("message", ex.getStatus() == HttpStatus.GATEWAY_TIMEOUT
                ? "O modelo de IA excedeu o tempo de resposta. Tente novamente ou ajuste o timeout da IA."
                : "Não foi possível concluir a inferência de IA. Tente novamente mais tarde.");
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    private HttpStatus resolveHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatus.valueOf(code);
        } catch (IllegalArgumentException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
