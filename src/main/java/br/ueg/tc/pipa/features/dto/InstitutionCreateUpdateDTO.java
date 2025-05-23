package br.ueg.tc.pipa.features.dto;

import br.ueg.tc.pipa_integrator.enums.Persona;

import java.util.List;

public record InstitutionCreateUpdateDTO(String shortName,
                                         String longName,
                                         List<Persona> personas,
                                         String salutationPhrase,
                                         String usernameFieldName,
                                         String passwordFieldName,
                                         String providerClass,
                                         String providerPath) {
}
