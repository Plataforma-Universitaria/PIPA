package br.ueg.tc.pipa.features.dto;

import br.ueg.tc.pipa_integrator.enums.Persona;

import java.util.List;

public record InstitutionLoginFieldsDTO(String salutationPhrase,
                                        String usernameFieldName,
                                        String passwordFieldName,
                                        List<Persona> personas,
                                        Integer status) {
}
