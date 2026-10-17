package br.ueg.tc.pipa.config;

import br.ueg.tc.pipa.features.ai.AiGatewayException;
import br.ueg.tc.pipa.features.observability.ProviderFailureResolver;
import br.ueg.tc.pipa_integrator.exceptions.institution.InstitutionCommunicationException;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureStage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerAiTest {
    @Test
    void nestedAiTimeoutKeepsItsOriginInHttpAndTelemetry() {
        var aiFailure = new AiGatewayException(HttpStatus.GATEWAY_TIMEOUT, "Tempo excedido");
        var wrapper = new InstitutionCommunicationException("Servidor UEG indisponível", aiFailure);
        var response = new GlobalExceptionHandler().handleBusinessException(wrapper);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).containsEntry("code", "AI_GATEWAY_TIMEOUT");
        assertThat(response.getBody().get("message").toString()).contains("modelo de IA").doesNotContain("UEG");
        var resolved = new ProviderFailureResolver().resolve(wrapper, ProviderFailureStage.TOOL_INVOCATION);
        assertThat(resolved.code()).isEqualTo("AI_GATEWAY_TIMEOUT");
        assertThat(resolved.stage()).isEqualTo(ProviderFailureStage.AI_INFERENCE);
        assertThat(resolved.retryable()).isTrue();
    }

    @Test
    void institutionFailureRetainsExistingHandling() {
        var response = new GlobalExceptionHandler().handleBusinessException(
                new InstitutionCommunicationException("Falha da UEG"));
        assertThat(response.getBody()).containsEntry("message", "Falha da UEG");
    }
}
