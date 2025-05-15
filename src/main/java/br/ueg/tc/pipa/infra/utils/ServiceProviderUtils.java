package br.ueg.tc.pipa.infra.utils;

import br.ueg.tc.pipa_integrator.serviceprovider.service.IServiceProvider;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ServiceProviderUtils {

    @Autowired
    private ApplicationContext context;

    public List<String> listAllServiceProviderBeans() {
        return  context.getBeansOfType(IServiceProvider.class)
                .values()
                .stream()
                .map(p -> p.getClass().getName())
                .toList();
    }
}