package br.ueg.tc.pipa.features.dto;

import br.ueg.tc.pipa_integrator.interfaces.providers.info.ILoginData;

import java.util.List;

public record InstitutionDTO(String shortName,
                             String longName,
                             List<ILoginData> loginData) {
}
