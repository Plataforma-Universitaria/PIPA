package br.ueg.tc.pipa.features.dto;

import br.ueg.tc.pipa.domain.institution.Persona;
import jakarta.persistence.Column;

import java.util.List;

public record InstitutionCreateUpdateDTO(String shortName,
                                         String longName,
                                         List<Persona> personas,
                                         String salutationPhrase,
                                         String usernameFieldName,
                                         String passwordFieldName,
                                         String providerClass) {
}
