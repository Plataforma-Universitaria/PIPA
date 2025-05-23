package br.ueg.tc.pipa.features.dto;

import br.ueg.tc.pipa_integrator.enums.Persona;

import java.util.List;

public record InstitutionDTO(String shortName,
                             String longName,
                             List<PersonaDTO> personas,
                             String salutationPhrase) {
}
