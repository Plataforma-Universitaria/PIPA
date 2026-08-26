package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa_integrator.observability.ProviderFailure;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureCategory;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureStage;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@Component
public class ProviderFailureResolver {

    private static final String FALLBACK_CODE = "UNCLASSIFIED_PROVIDER_FAILURE";

    /**
     * Percorre a cadeia de causas e privilegia a falha tipada mais próxima da
     * causa raiz. Assim, wrappers técnicos não apagam a interpretação feita
     * pelo provider.
     */
    public ResolvedProviderFailure resolve(Throwable failure, ProviderFailureStage fallbackStage) {
        ProviderFailure resolved = null;
        Throwable resolvedThrowable = null;
        Throwable current = failure;
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        while (current != null && visited.add(current)) {
            if (current instanceof ProviderFailure providerFailure) {
                resolved = providerFailure;
                resolvedThrowable = current;
            }
            current = current.getCause();
        }

        if (resolved != null) {
            return new ResolvedProviderFailure(
                    resolved.errorCode(),
                    resolved.category(),
                    resolved.stage(),
                    resolved.retryable(),
                    resolvedThrowable.getClass().getSimpleName()
            );
        }

        ProviderFailureStage safeFallbackStage = fallbackStage != null
                ? fallbackStage
                : ProviderFailureStage.UNKNOWN;
        return new ResolvedProviderFailure(
                FALLBACK_CODE,
                ProviderFailureCategory.UNKNOWN,
                safeFallbackStage,
                false,
                failure != null ? failure.getClass().getSimpleName() : null
        );
    }
}
