package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.features.observability.dto.AiUsageDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiUsageControllerTest {
    @Test
    void bindsPeriodAndOriginAndReturnsOnlyAggregateCounts() throws Exception {
        var service = new CapturingService();
        var mvc = MockMvcBuilders.standaloneSetup(new AiUsageController(service)).build();
        mvc.perform(get("/api/observability/ai-usage")
                        .param("from", "2026-10-14T00:00:00")
                        .param("to", "2026-10-14T23:59:59.999")
                        .param("aiProvider", "local")
                        .param("persona", "Aluno")
                        .param("institution", "UEG")
                        .param("toolName", "consultar_notas"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"totalInferences":3,"inferencesWithTokenUsage":2,"totalTokens":1500,
                         "inputTokens":1200,"outputTokens":300,
                         "inferencesWithInputTokens":2,"inferencesWithOutputTokens":1}
                        """, true));
        assertThat(service.from).isEqualTo(LocalDateTime.parse("2026-10-14T00:00:00"));
        assertThat(service.to).isEqualTo(LocalDateTime.parse("2026-10-14T23:59:59.999"));
        assertThat(service.provider).isEqualTo("local");
        assertThat(service.persona).isEqualTo("Aluno");
        assertThat(service.institution).isEqualTo("UEG");
        assertThat(service.toolName).isEqualTo("consultar_notas");
    }

    @Test
    void rejectsInvalidOriginAndPeriodThroughHttp() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new AiUsageController(new AiUsageService(null))).build();
        mvc.perform(get("/api/observability/ai-usage").param("aiProvider", "ueg-provider"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/observability/ai-usage")
                        .param("from", "2026-10-15T00:00:00").param("to", "2026-10-14T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    private static class CapturingService extends AiUsageService {
        private LocalDateTime from;
        private LocalDateTime to;
        private String provider;
        private String persona;
        private String institution;
        private String toolName;

        CapturingService() { super(null); }

        @Override
        public AiUsageDTO getUsage(LocalDateTime from, LocalDateTime to, String aiProvider,
                                   String persona, String institution, String toolName) {
            this.from = from;
            this.to = to;
            this.provider = aiProvider;
            this.persona = persona;
            this.institution = institution;
            this.toolName = toolName;
            return new AiUsageDTO(3, 2, 1500L, 1200L, 300L, 2, 1);
        }
    }
}
