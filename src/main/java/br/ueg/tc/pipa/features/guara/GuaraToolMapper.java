package br.ueg.tc.pipa.features.guara;

import br.ueg.tc.pipa.features.dto.GuaraToolDTO;
import br.ueg.tc.pipa.features.dto.ParameterDTO;
import br.ueg.tc.pipa.infra.utils.ServiceProviderUtils;
import br.ueg.tc.pipa_integrator.annotations.ServiceProviderMethod;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GuaraToolMapper {

    public List<GuaraToolDTO> mapTools(String providerPath, List<String> personas) {
        List<GuaraToolDTO> tools = new ArrayList<>();

        // Obter todas as classes de serviço baseadas no provedor e nas personas
        List<String> serviceNames = ServiceProviderUtils.listAllProviderServicesByProvider(providerPath, personas);

        for (String serviceName : serviceNames) {
            try {
                Class<?> clazz = Class.forName(serviceName);
                Method[] methods = clazz.getDeclaredMethods();

                for (Method method : methods) {
                    if (method.isAnnotationPresent(ServiceProviderMethod.class)) {
                        ServiceProviderMethod annotation = method.getAnnotation(ServiceProviderMethod.class);

                        String name = annotation.actionName();

                        StringBuilder descriptionBuilder = new StringBuilder();
                        if (annotation.activationPhrases() != null && annotation.activationPhrases().length > 0) {
                            descriptionBuilder.append("Ativação: ").append(String.join(", ", annotation.activationPhrases()));
                        }
                        if (annotation.addSpec() != null && annotation.addSpec().length > 0 && !annotation.addSpec()[0].isEmpty()) {
                            if (!descriptionBuilder.isEmpty()) {
                                descriptionBuilder.append(" | ");
                            }
                            descriptionBuilder.append("Especificação: ").append(String.join(", ", annotation.addSpec()));
                        }

                        String description = descriptionBuilder.toString();
                        if (description.isEmpty()) {
                            description = "Executa a ação " + name;
                        }

                        boolean highConfirmation = annotation.manipulatesData();
                        boolean authenticationRequired = !personas.contains("Convidado");

                        Map<String, ParameterDTO> parameters = new HashMap<>();
                        for (Parameter param : method.getParameters()) {
                            parameters.put(param.getName(), mapParameter(param));
                        }

                        tools.add(new GuaraToolDTO(name, description, highConfirmation, authenticationRequired, parameters));
                    }
                }
            } catch (ClassNotFoundException e) {
                // Classe não encontrada, ignoramos silenciosamente para seguir o fluxo
            }
        }

        return tools;
    }

    private ParameterDTO mapParameter(Parameter param) {
        String type = "MANDATORY";
        String clazzType = "STRING";
        List<String> possibleValues = null;

        Class<?> pType = param.getType();
        if (pType == int.class || pType == Integer.class ||
                pType == double.class || pType == Double.class ||
                pType == long.class || pType == Long.class) {
            clazzType = "NUMBER";
        } else if (pType == boolean.class || pType == Boolean.class) {
            clazzType = "BOOLEAN";
        } else if (pType.isEnum()) {
            clazzType = "ENUM";
            possibleValues = new ArrayList<>();
            for (Object enumConstant : pType.getEnumConstants()) {
                possibleValues.add(enumConstant.toString());
            }
        }

        String description = splitCamelCase(param.getName());

        return new ParameterDTO(type, clazzType, description, possibleValues);
    }

    private String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        ).toLowerCase();
    }
}
