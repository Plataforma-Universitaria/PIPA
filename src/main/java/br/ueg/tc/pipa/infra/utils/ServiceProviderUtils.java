package br.ueg.tc.pipa.infra.utils;

import br.ueg.tc.pipa_integrator.annotations.ServiceProviderClass;
import br.ueg.tc.pipa_integrator.serviceprovider.service.IServiceProvider;
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


    public static List<String> listAllMethodsByServiceProvider(String serviceProvider) {
        Class<? extends IServiceProvider> clazz = getServiceProviderByName(serviceProvider);

        List<Method> methods = new ArrayList<>(List.of(clazz.getDeclaredMethods()));

        if (!clazz.getSuperclass().getSimpleName().equals("Object")) {
            methods.addAll(List.of(clazz.getSuperclass().getDeclaredMethods()));
        }

        return methods.stream().map(Method::getName).collect(Collectors.toList());
    }


}
