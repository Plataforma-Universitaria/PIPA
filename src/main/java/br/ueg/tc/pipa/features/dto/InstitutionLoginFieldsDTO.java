package br.ueg.tc.pipa.features.dto;

import java.util.List;

public record InstitutionLoginFieldsDTO(String salutationPhrase,
                                        String usernameFieldName,
                                        String passwordFieldName,
                                        List<String> personas,
                                        Integer status) {
}
