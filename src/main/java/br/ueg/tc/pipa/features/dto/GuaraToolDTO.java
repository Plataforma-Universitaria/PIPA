package br.ueg.tc.pipa.features.dto;

import java.util.Map;

public record GuaraToolDTO(
    String name,
    String description,
    boolean highConfirmation,
    boolean authenticationRequired,
    Map<String, ParameterDTO> parameters
) {}
