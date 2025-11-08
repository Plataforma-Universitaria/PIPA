package br.ueg.tc.pipa.infra.utils;

import br.ueg.tc.pipa.domain.institution.Institution;
import br.ueg.tc.pipa.publicServices.HelpService;
import br.ueg.tc.pipa.publicServices.PublicService;
import br.ueg.tc.pipa_integrator.annotations.ServiceProviderClass;
import br.ueg.tc.pipa_integrator.annotations.ServiceProviderMethod;
import br.ueg.tc.pipa_integrator.exceptions.institution.InstitutionServiceException;
import br.ueg.tc.pipa_integrator.interfaces.providers.IBaseInstitutionProvider;
import br.ueg.tc.pipa_integrator.interfaces.providers.service.IServiceProvider;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ServiceProviderUtils {

    private static String prefixPackage = "br.ueg.tc";

    public static Set<String> listAllServiceProviderBeans() {
        Reflections reflections = new Reflections(prefixPackage);

        Set<Class<? extends IServiceProvider>> implementations = reflections.getSubTypesOf(IServiceProvider.class);
        return implementations.stream()
                .map(Class::getName)
                .collect(Collectors.toSet());
    }

    public static Class<? extends IServiceProvider> getServiceProviderByName(String serviceName) {
        Reflections reflections = new Reflections(prefixPackage);

        Class<? extends IServiceProvider> implementation = reflections.getSubTypesOf(IServiceProvider.class)
                .stream()
                .filter(t -> t.getName().contains(serviceName))
                .findFirst().orElse(null);

        return implementation;
    }

    public static List<String> listAllProviderServicesByProvider(String provider) {
        return listAllServiceProviderBeans().stream().filter(s ->
                s.contains(provider)).collect(Collectors.toList());
    }

    public static List<String> listAllProviderServicesByProvider(String provider, List<String> personas) {
        return listAllServiceProviderBeans().stream()
                .filter(beanName -> beanName.contains(provider))
                .map(ServiceProviderUtils::getServiceProviderByName)
                .filter(Objects::nonNull)
                .filter(clazz -> clazz.isAnnotationPresent(ServiceProviderClass.class))
                .filter(clazz -> {
                    ServiceProviderClass annotation = clazz.getAnnotation(ServiceProviderClass.class);
                    List<String> classPersonas = Arrays.asList(annotation.personas());
                    return classPersonas.stream().anyMatch(personas::contains);
                })
                .map(Class::getName)
                .collect(Collectors.toList());
    }

    public static List<Method> listAllPublicServicesFromPipa() {
        Class<? extends IServiceProvider> clazz = PublicService.class;
        List<Method> methods = new ArrayList<>(List.of(clazz.getDeclaredMethods()));
        return listAllServiceMethods(methods);

    }

    public static List<Method> listAllServiceMethods(List<Method> methods) {
        return methods.stream()
                .filter(method -> method.isAnnotationPresent(ServiceProviderMethod.class))
                .collect(Collectors.toList());
    }


    public static List<String> listAllMethodsByServiceProvider(String serviceProvider) {
        Class<? extends IServiceProvider> clazz = getServiceProviderByName(serviceProvider);

        List<Method> methods = new ArrayList<>(List.of(clazz.getDeclaredMethods()));

        if (!clazz.getSuperclass().getSimpleName().equals("Object")) {
            methods.addAll(List.of(clazz.getSuperclass().getDeclaredMethods()));
        }

        return methods.stream().map(Method::getName).collect(Collectors.toList());
    }

    public static String getMethodsDescription(List<Method> methods) {
        StringBuilder description = new StringBuilder();
        for (Method method : methods) {
            description.append("\n  Assinatura do Método: ").append(method.getName()).append("(")
                    .append(String.join(", ",
                            Arrays.stream(method.getParameterTypes())
                                    .map(Class::getSimpleName)
                                    .toList()))
                    .append(") - Exemplos: ")
                    .append(Arrays.toString(method.getAnnotation(ServiceProviderMethod.class).activationPhrases()));
            if (method.getAnnotation(ServiceProviderMethod.class).manipulatesData()) {
                description.append("OBS: Este método **manipula dados críticos**. " +
                        "Só deve ser ativado se houver **mais de 97% de certeza** de que a intenção do usuário corresponde exatamente a este método. " +
                        "Se houver qualquer dúvida, NÃO ative e responda solicitando confirmação.\n");
            }
            if (Arrays.stream(method.getAnnotation(ServiceProviderMethod.class).addSpec()).toList().size() > 1) {
                description.append("Este método tem parâmetros e essa é a especificação deles:\n").append(Arrays.toString(method.getAnnotation(ServiceProviderMethod.class).addSpec()));
            }

        }
        return description.toString();
    }

    public static List<String> getActionNamesByPersonaAndInstitution(List<String> personas, Institution institution, IBaseInstitutionProvider institutionProvider) {
        String provider;
        try {
            provider = institution.getProviderPath();
        } catch (Exception e) {
            throw new InstitutionServiceException("Não foi possível encontrar a instituição.");
        }


        List<String> services = listAllProviderServicesByProvider(provider, personas);
        if(institutionProvider.canAccessTask().stream().anyMatch(personas::contains)) {
            services.add(PublicService.class.getName());
        }
        services.add(HelpService.class.getName());
        List<Class<? extends IServiceProvider>> matchingClasses = services.stream()
                .map(ServiceProviderUtils::getServiceProviderByName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return matchingClasses.stream()
                .flatMap(clazz -> {
                    List<Method> methods = new ArrayList<>(List.of(clazz.getDeclaredMethods()));
                    if (!clazz.getSuperclass().getSimpleName().equals("Object")) {
                        methods.addAll(List.of(clazz.getSuperclass().getDeclaredMethods()));
                    }
                    return methods.stream();
                })
                .filter(method -> method.isAnnotationPresent(ServiceProviderMethod.class))
                .map(method -> method.getAnnotation(ServiceProviderMethod.class).actionName())
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Method> listAllHelpServices() {
        Class<? extends IServiceProvider> clazz = HelpService.class;
        List<Method> methods = new ArrayList<>(List.of(clazz.getDeclaredMethods()));
        return listAllServiceMethods(methods);
    }
}
