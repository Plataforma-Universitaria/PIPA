package br.ueg.tc.pipa.features.dto;

import java.time.LocalDateTime;

public record DiaryDTO(String note, LocalDateTime date, Long userUuid) {
}
