package br.ueg.tc.pipa.features.dto;

public record LoginDTO(
        String username,
        String password,
        String persona,
        String institutionName) {
}
