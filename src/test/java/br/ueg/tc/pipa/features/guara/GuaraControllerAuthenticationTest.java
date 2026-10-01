package br.ueg.tc.pipa.features.guara;

import br.ueg.tc.pipa.features.dto.GuaraToolDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuaraControllerAuthenticationTest {

    private GuaraController controller;
    private RecordingGuaraService service;
    private String userExternalId;

    @BeforeEach
    void setUp() {
        controller = new GuaraController();
        service = new RecordingGuaraService();
        ReflectionTestUtils.setField(controller, "guaraService", service);
        userExternalId = UUID.randomUUID().toString();
    }

    @Test
    void rejectsToolDiscoveryWithoutUserJwt() {
        assertThatThrownBy(() -> controller.listTools(userExternalId, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void rejectsToolExecutionForAnotherUser() {
        assertThatThrownBy(() -> controller.executeTool(
                "adicionar_uma_anotacao", userExternalId, "", "TELEGRAM", Map.of(), jwt(UUID.randomUUID().toString())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void allowsToolExecutionForJwtSubject() {
        controller.executeTool("adicionar_uma_anotacao", userExternalId,
                "session", "TELEGRAM", Map.of("note", "teste"), jwt(userExternalId));

        assertThat(service.executedTool).isEqualTo("adicionar_uma_anotacao");
        assertThat(service.executedUser).isEqualTo(userExternalId);
        assertThat(service.executedParams).isEqualTo(Map.of("note", "teste"));
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(subject)
                .build();
    }

    private static class RecordingGuaraService extends GuaraService {
        private String executedTool;
        private String executedUser;
        private Map<String, String> executedParams;

        @Override
        public List<GuaraToolDTO> listTools(String userExternalId) {
            return List.of();
        }

        @Override
        public Object executeTool(String toolName, String userExternalId,
                                  Map<String, String> params, String sessionId, String channel) {
            executedTool = toolName;
            executedUser = userExternalId;
            executedParams = params;
            return "ok";
        }
    }
}
