package br.ueg.tc.pipa.features.observability.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Resposta paginada estável, independente da representação interna do Spring Data. */
public record PageResponseDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponseDTO<T> from(Page<T> source) {
        return new PageResponseDTO<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast()
        );
    }
}
