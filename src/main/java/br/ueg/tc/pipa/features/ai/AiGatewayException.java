package br.ueg.tc.pipa.features.ai;

import org.springframework.http.HttpStatus;

public class AiGatewayException extends RuntimeException {
    private final HttpStatus status;

    public AiGatewayException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
