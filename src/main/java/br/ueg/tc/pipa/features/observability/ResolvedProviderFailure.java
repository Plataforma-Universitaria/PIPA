package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa_integrator.observability.ProviderFailureCategory;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureStage;

public record ResolvedProviderFailure(
        String code,
        ProviderFailureCategory category,
        ProviderFailureStage stage,
        boolean retryable,
        String safeDetails
) {
}
