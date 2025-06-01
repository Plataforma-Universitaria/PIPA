package br.ueg.tc.pipa.features.dto;

import java.util.List;

public record InstitutionDTO(String shortName,
                             String longName,
                             List<String> personas,
                             String salutationPhrase) {
}
