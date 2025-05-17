package br.ueg.tc.pipa.features.dto;

import br.ueg.tc.pipa.domain.institution.Persona;

public record LoginDTO(
        String username,
        String password,
        Persona persona,
        String institutionName) {
}
