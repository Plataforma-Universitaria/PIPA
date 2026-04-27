package br.ueg.tc.pipa.features.dto;

import java.util.List;

public record ParameterDTO(
    String type,
    String clazz,
    String description,
    List<String> possibleValues
) {}
