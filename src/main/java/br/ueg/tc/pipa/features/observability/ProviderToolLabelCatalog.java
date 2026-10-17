package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.features.guara.GuaraToolMapper;
import br.ueg.tc.pipa.infra.utils.ServiceProviderUtils;
import br.ueg.tc.pipa_integrator.annotations.ServiceProviderMethod;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Rótulos derivados das mesmas anotações que definem as ferramentas executáveis. */
@Component
public class ProviderToolLabelCatalog {
    private final Map<String, String> labels;

    public ProviderToolLabelCatalog() {
        this(ServiceProviderUtils.listAllServiceProviderBeans().stream().sorted()
                .map(ProviderToolLabelCatalog::loadProviderClass).toList());
    }

    ProviderToolLabelCatalog(Collection<Class<?>> providerClasses) {
        Map<String, String> discovered = new LinkedHashMap<>();
        for (Class<?> providerClass : providerClasses) {
            for (var method : providerClass.getMethods()) {
                var annotation = method.getAnnotation(ServiceProviderMethod.class);
                if (annotation == null || annotation.actionName().isBlank()) continue;
                discovered.putIfAbsent(GuaraToolMapper.sanitizeToolName(annotation.actionName()),
                        annotation.actionName());
            }
        }
        labels = Map.copyOf(discovered);
    }

    public Map<String, String> labelsFor(Collection<String> toolNames) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String toolName : toolNames) {
            String label = labels.get(toolName);
            if (label != null) result.put(toolName, label);
        }
        return result;
    }

    private static Class<?> loadProviderClass(String className) {
        try {
            return Class.forName(className, false, ProviderToolLabelCatalog.class.getClassLoader());
        } catch (ClassNotFoundException cause) {
            throw new IllegalStateException("Classe de provedor descoberta não pôde ser carregada", cause);
        }
    }
}
