package br.ueg.tc.pipa.features.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class PipaAiInferenceClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AiGatewayService gateway = mock(AiGatewayService.class);
    private final PipaAiInferenceClient client = new PipaAiInferenceClient(gateway, mapper);

    @Test
    void sendsStructuredPlanThroughGateway() throws Exception {
        ObjectNode body = response("{\"serviceName\":\"Example\"}", "stop");
        when(gateway.complete(any())).thenReturn(new AiGatewayService.GatewayResult(
                body, "local", "test", "request-1"));

        String answer = client.completeJson("intenção", "classifique", "{\"type\":\"object\"}");

        assertEquals("{\"serviceName\":\"Example\"}", answer);
        ArgumentCaptor<ObjectNode> request = ArgumentCaptor.forClass(ObjectNode.class);
        verify(gateway).complete(request.capture());
        assertEquals("classifique", request.getValue().path("messages").get(0).path("content").asText());
        assertEquals("intenção", request.getValue().path("messages").get(1).path("content").asText());
        assertEquals("json_schema", request.getValue().path("response_format").path("type").asText());
        assertEquals("object", request.getValue().path("response_format").path("json_schema")
                .path("schema").path("type").asText());
    }

    @Test
    void rejectsTruncatedResponseInsteadOfReturningPartialPlan() throws Exception {
        ObjectNode body = response("{\"serviceName\":", "length");
        when(gateway.complete(any())).thenReturn(new AiGatewayService.GatewayResult(
                body, "local", "test", "request-2"));

        assertEquals(HttpStatus.BAD_GATEWAY,
                assertThrows(AiGatewayException.class, () -> client.complete("pergunta")).getStatus());
    }

    private ObjectNode response(String content, String finishReason) {
        ObjectNode body = mapper.createObjectNode();
        ObjectNode choice = body.putArray("choices").addObject();
        choice.putObject("message").put("content", content);
        choice.put("finish_reason", finishReason);
        return body;
    }
}
