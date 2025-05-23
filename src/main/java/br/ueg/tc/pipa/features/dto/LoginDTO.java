package br.ueg.tc.pipa.features.dto;

import br.ueg.tc.pipa_integrator.enums.Persona;

public record LoginDTO(
        String username,
        String password,
        String persona,
        String institutionName) {
}
