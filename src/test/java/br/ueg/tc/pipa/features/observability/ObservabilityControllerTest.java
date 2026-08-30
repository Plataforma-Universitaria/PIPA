package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.features.observability.dto.ObservabilityLogDTO;
import br.ueg.tc.pipa.features.observability.dto.PageResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityControllerTest {

    @Test
    void shouldBuildSingleFilterAndReturnStablePageEnvelope() {
        CapturingObservabilityService service = new CapturingObservabilityService();
        ObservabilityController controller = new ObservabilityController(service);
        LocalDateTime from = LocalDateTime.of(2026, 8, 25, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 25, 12, 0);
        Pageable pageable = Pageable.ofSize(20);

        ResponseEntity<PageResponseDTO<ObservabilityLogDTO>> response = controller.getLogs(
                "legacy-session", 42L, "consultar_notas", "Aluno", "UEG",
                "ueg-provider", "TELEGRAM", "Sucesso", from, to, pageable
        );

        assertThat(service.filter).isEqualTo(new ObservabilityFilter(
                from, to, "Aluno", "consultar_notas", "UEG", "ueg-provider",
                "TELEGRAM", "Sucesso", 42L, "legacy-session"
        ));
        assertThat(service.pageable).isSameAs(pageable);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).containsExactly(service.dto);
        assertThat(response.getBody().totalElements()).isEqualTo(1);
    }

    private static class CapturingObservabilityService extends ObservabilityService {

        private final ObservabilityLogDTO dto = new ObservabilityLogDTO(
                1L, "consultar_notas", "1.0", "Aluno", "Sucesso", 15L,
                null, null, null, null, LocalDateTime.of(2026, 8, 25, 11, 0)
        );
        private ObservabilityFilter filter;
        private Pageable pageable;

        private CapturingObservabilityService() {
            super(null, null, null, null, null, null, new ObservabilitySessionProperties());
        }

        @Override
        public Page<ObservabilityLogDTO> getLogs(ObservabilityFilter filter, Pageable pageable) {
            this.filter = filter;
            this.pageable = pageable;
            return new PageImpl<>(List.of(dto), pageable, 1);
        }
    }
}
