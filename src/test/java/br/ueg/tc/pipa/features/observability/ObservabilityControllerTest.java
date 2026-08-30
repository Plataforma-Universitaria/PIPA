package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.features.observability.dto.ObservabilityLogDTO;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityFilterOptionsDTO;
import br.ueg.tc.pipa.features.observability.dto.ObservabilityDashboardDTO;
import br.ueg.tc.pipa.features.observability.dto.PageResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityControllerTest {

    @Test
    void shouldBuildSingleFilterAndReturnStablePageEnvelope() {
        CapturingObservabilityService service = new CapturingObservabilityService();
        ObservabilityController controller = new ObservabilityController(service, new CapturingExportService());
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

    @Test
    void shouldReturnFilterOptionsContract() {
        CapturingObservabilityService service = new CapturingObservabilityService();
        ObservabilityController controller = new ObservabilityController(service, new CapturingExportService());

        ResponseEntity<ObservabilityFilterOptionsDTO> response = controller.getFilterOptions();

        assertThat(response.getBody()).isEqualTo(service.options);
    }

    @Test
    void shouldReuseTheCompleteFilterForDashboard() {
        CapturingObservabilityService service = new CapturingObservabilityService();
        ObservabilityController controller = new ObservabilityController(service, new CapturingExportService());
        LocalDateTime from = LocalDateTime.of(2026, 8, 29, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 30, 23, 59);

        ResponseEntity<ObservabilityDashboardDTO> response = controller.getDashboard(
                "legacy-session", 42L, "consultar_notas", "Aluno", "UEG",
                "ueg-provider", "TELEGRAM", "Sucesso", from, to);

        assertThat(service.filter).isEqualTo(new ObservabilityFilter(
                from, to, "Aluno", "consultar_notas", "UEG", "ueg-provider",
                "TELEGRAM", "Sucesso", 42L, "legacy-session"));
        assertThat(response.getBody()).isEqualTo(service.dashboard);
    }

    @Test
    void shouldExportWithAttachmentHeadersAndCompleteFilter() {
        CapturingObservabilityService service = new CapturingObservabilityService();
        CapturingExportService exportService = new CapturingExportService();
        ObservabilityController controller = new ObservabilityController(service, exportService);
        LocalDateTime from = LocalDateTime.of(2026, 8, 29, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 30, 23, 59);

        ResponseEntity<byte[]> response = controller.export(
                "pdf", "legacy-session", 42L, "consultar_notas", "Aluno", "UEG",
                "ueg-provider", "TELEGRAM", "Sucesso", from, to);

        assertThat(exportService.format).isEqualTo("pdf");
        assertThat(exportService.filter).isEqualTo(new ObservabilityFilter(
                from, to, "Aluno", "consultar_notas", "UEG", "ueg-provider",
                "TELEGRAM", "Sucesso", 42L, "legacy-session"));
        assertThat(response.getHeaders().getContentType()).hasToString("application/pdf");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("attachment; filename=\"observabilidade-")
                .endsWith(".pdf\"");
        assertThat(response.getBody()).containsExactly(1, 2, 3);
    }

    private static class CapturingObservabilityService extends ObservabilityService {

        private final ObservabilityLogDTO dto = new ObservabilityLogDTO(
                1L, "consultar_notas", "1.0", "Aluno", "Sucesso", 15L,
                null, null, null, null, LocalDateTime.of(2026, 8, 25, 11, 0)
        );
        private final ObservabilityFilterOptionsDTO options = new ObservabilityFilterOptionsDTO(
                List.of("Aluno"), List.of("consultar_notas"), List.of("UEG"),
                List.of("ueg-provider"), List.of("TELEGRAM"), List.of("Sucesso", "Falha")
        );
        private final ObservabilityDashboardDTO dashboard = new ObservabilityDashboardDTO(
                new ObservabilityDashboardDTO.Summary(1L, null, 15.0, 1L, 100.0),
                List.of(new ObservabilityDashboardDTO.ToolUsage("consultar_notas", 1L)),
                List.of(), List.of(), List.of());
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

        @Override
        public ObservabilityFilterOptionsDTO getFilterOptions() {
            return options;
        }

        @Override
        public ObservabilityDashboardDTO getDashboard(ObservabilityFilter filter) {
            this.filter = filter;
            return dashboard;
        }
    }

    private static class CapturingExportService extends ObservabilityExportService {
        private ObservabilityFilter filter;
        private String format;

        private CapturingExportService() {
            super(null, null);
        }

        @Override
        public ExportedFile export(ObservabilityFilter filter, String requestedFormat) {
            this.filter = filter;
            this.format = requestedFormat;
            return new ExportedFile(new byte[]{1, 2, 3}, "application/pdf", "pdf");
        }
    }
}
