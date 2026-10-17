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
                        .param("aiProvider", "local"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"totalInferences\":3,\"inferencesWithTokenUsage\":2,\"totalTokens\":1500}", true));
        assertThat(service.from).isEqualTo(LocalDateTime.parse("2026-10-14T00:00:00"));
        assertThat(service.to).isEqualTo(LocalDateTime.parse("2026-10-14T23:59:59.999"));
        assertThat(service.provider).isEqualTo("local");
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

        CapturingService() { super(null); }

        @Override
        public AiUsageDTO getUsage(LocalDateTime from, LocalDateTime to, String aiProvider) {
            this.from = from;
            this.to = to;
            this.provider = aiProvider;
            return new AiUsageDTO(3, 2, 1500L);
        }
    }
}
