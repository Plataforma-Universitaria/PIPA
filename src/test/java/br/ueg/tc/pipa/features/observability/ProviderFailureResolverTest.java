package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa_integrator.exceptions.institution.InstitutionCommunicationException;
import br.ueg.tc.pipa_integrator.exceptions.user.UserNotFoundException;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureCategory;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderFailureResolverTest {

    private final ProviderFailureResolver resolver = new ProviderFailureResolver();

    @Test
    void shouldPreferDeepestTypedCause() {
        Throwable failure = new InstitutionCommunicationException(
                "mensagem externa segura",
                new UserNotFoundException()
        );

        ResolvedProviderFailure resolved = resolver.resolve(failure, ProviderFailureStage.TOOL_INVOCATION);

        assertThat(resolved.code()).isEqualTo("PROVIDER_USER_NOT_FOUND");
        assertThat(resolved.category()).isEqualTo(ProviderFailureCategory.AUTHENTICATION);
        assertThat(resolved.stage()).isEqualTo(ProviderFailureStage.USER_RESOLUTION);
        assertThat(resolved.retryable()).isFalse();
        assertThat(resolved.safeDetails()).isEqualTo("UserNotFoundException");
    }

    @Test
    void shouldNormalizeUntypedFailureWithoutPersistingItsMessage() {
        ResolvedProviderFailure resolved = resolver.resolve(
                new IllegalStateException("token=segredo"),
                ProviderFailureStage.TOOL_INVOCATION
        );

        assertThat(resolved.code()).isEqualTo("UNCLASSIFIED_PROVIDER_FAILURE");
        assertThat(resolved.category()).isEqualTo(ProviderFailureCategory.UNKNOWN);
        assertThat(resolved.stage()).isEqualTo(ProviderFailureStage.TOOL_INVOCATION);
        assertThat(resolved.safeDetails()).isEqualTo("IllegalStateException");
    }
}
