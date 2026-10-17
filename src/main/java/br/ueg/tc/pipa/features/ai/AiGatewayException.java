package br.ueg.tc.pipa.features.ai;

import org.springframework.http.HttpStatus;
import br.ueg.tc.pipa_integrator.observability.ProviderFailure;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureCategory;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureStage;

public class AiGatewayException extends RuntimeException implements ProviderFailure {
    private final HttpStatus status;

    public AiGatewayException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String errorCode() { return "AI_" + status.name(); }

    @Override
    public ProviderFailureCategory category() {
        return status == HttpStatus.BAD_REQUEST || status == HttpStatus.PAYLOAD_TOO_LARGE
                ? ProviderFailureCategory.VALIDATION : ProviderFailureCategory.COMMUNICATION;
    }

    @Override
    public ProviderFailureStage stage() { return ProviderFailureStage.AI_INFERENCE; }

    @Override
    public boolean retryable() { return status.is5xxServerError(); }
}
