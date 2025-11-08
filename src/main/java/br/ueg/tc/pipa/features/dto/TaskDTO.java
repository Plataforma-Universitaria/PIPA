package br.ueg.tc.pipa.features.dto;

import java.time.LocalDateTime;

public record TaskDTO(String note, LocalDateTime date, Long userUuid) {
}
