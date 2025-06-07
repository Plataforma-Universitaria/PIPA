package br.ueg.tc.pipa.features.dto;

import br.ueg.tc.pipa_integrator.interfaces.providers.service.IServiceProvider;

import java.util.List;

public record ServiceAndActivationNames(
        IServiceProvider service,
        String serviceName,
        List<String> activationNames) {
}
