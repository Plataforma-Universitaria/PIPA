package br.ueg.tc.pipa.features.dto;

import java.util.List;

public record LoginDTO(
        String username,
        String password,
        List<String> personas,
        String institutionName) {
}
