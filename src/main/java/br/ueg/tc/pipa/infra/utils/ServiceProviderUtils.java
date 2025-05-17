package br.ueg.tc.pipa.infra.utils;

import br.ueg.tc.pipa_integrator.serviceprovider.service.IServiceProvider;
import org.reflections.Reflections;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ServiceProviderUtils {

//TODO: Ajustar prefix pegando do config

//    @Value("${prefix-package}")
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

    public static List<String> listAllMethodsByServiceProvider(String serviceProvider) {
        Class<? extends IServiceProvider> clazz = getServiceProviderByName(serviceProvider);

        List<Method> methods = new ArrayList<>(List.of(clazz.getDeclaredMethods()));

        if(!clazz.getSuperclass().getSimpleName().equals("Object")) {
            methods.addAll(List.of(clazz.getSuperclass().getDeclaredMethods()));
        }

        return methods.stream().map(Method::getName).collect(Collectors.toList());
    }

    public static class DependencyInfo {
        private final String groupId;
        private final String artifactId;
        private final String version;

        public DependencyInfo(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        @Override
        public String toString() {
            return String.format("groupId: %s, artifactId: %s, version: %s", groupId, artifactId, version);
        }
    }



}
