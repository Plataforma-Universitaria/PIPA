package br.ueg.tc.pipa.features.ai;

import br.ueg.tc.pipa.features.observability.AiInferenceEvent;
import br.ueg.tc.pipa.features.observability.AiInferenceTelemetryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiGatewayServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AiInferenceTelemetryService telemetry = mock(AiInferenceTelemetryService.class);

    @Test
    void externalOnlyPreservesToolCallsAndOverridesRequestedModel() throws IOException {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<JsonNode> forwarded = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            forwarded.set(mapper.readTree(exchange.getRequestBody()));
            respond(exchange, 200, """
                    {"id":"completion-1","object":"chat.completion","created":1,"model":"external-test",
                     "choices":[{"index":0,"message":{"role":"assistant","content":null,
                     "tool_calls":[{"id":"call-1","type":"function","function":{"name":"consultar","arguments":"{}"}}]},
                     "finish_reason":"tool_calls"}],"usage":{"prompt_tokens":10,"completion_tokens":3,"total_tokens":13}}
                    """);
        });
        try {
            AiGatewayProperties properties = properties("EXTERNAL_ONLY", server);
            properties.setExternalModel("external-test");
            properties.setExternalApiKey("test-secret");
            AiGatewayService service = new AiGatewayService(properties, mapper, telemetry);
            ObjectNode request = request();
            request.put("model", "modelo-escolhido-pelo-cliente");
            request.set("tools", mapper.readTree("[{\"type\":\"function\",\"function\":{\"name\":\"consultar\",\"parameters\":{\"type\":\"object\"}}}]"));

            AiGatewayService.GatewayResult result = service.complete(request);

            assertEquals("external", result.provider());
            assertEquals("Bearer test-secret", authorization.get());
            assertEquals("external-test", forwarded.get().path("model").asText());
            assertFalse(forwarded.get().has("reasoning_effort"));
            assertEquals(false, forwarded.get().path("stream").asBoolean());
            assertEquals(request.path("messages"), forwarded.get().path("messages"));
            assertEquals("consultar", forwarded.get().path("tools").get(0).path("function").path("name").asText());
            assertEquals("call-1", result.body().path("choices").get(0)
                    .path("message").path("tool_calls").get(0).path("id").asText());
            org.mockito.ArgumentCaptor<AiInferenceEvent> event = org.mockito.ArgumentCaptor.forClass(AiInferenceEvent.class);
            verify(telemetry).recordSafely(event.capture());
            assertEquals("SUCCESS", event.getValue().result());
            assertEquals(10L, event.getValue().inputTokens());
            assertEquals(3L, event.getValue().outputTokens());
            assertEquals(13L, event.getValue().totalTokens());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void localReasoningIsConfiguredByPipaAndToolMessagesArePreserved() throws IOException {
        AtomicReference<JsonNode> forwarded = new AtomicReference<>();
        HttpServer local = server(exchange -> {
            forwarded.set(mapper.readTree(exchange.getRequestBody()));
            respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"ok\"},\"finish_reason\":\"stop\"}]}");
        });
        try {
            AiGatewayProperties properties = properties("LOCAL_ONLY", local);
            properties.setLocalBaseUrl(baseUrl(local));
            properties.setLocalModel("qwen-test");
            properties.setLocalReasoningEffort("none");
            properties.setLocalTemperature(0.6);
            properties.setLocalTopP(0.95);
            properties.setExternalApiKey("unused");
            ObjectNode request = request();
            request.put("reasoning_effort", "high");
            request.put("temperature", 0);
            request.withArray("messages").addObject().put("role", "tool")
                    .put("tool_call_id", "call-1").put("content", "resultado");
            new AiGatewayService(properties, mapper, telemetry).complete(request);
            assertEquals("none", forwarded.get().path("reasoning_effort").asText());
            assertEquals(0.6, forwarded.get().path("temperature").asDouble());
            assertEquals(0.95, forwarded.get().path("top_p").asDouble());
            assertEquals("call-1", forwarded.get().path("messages").get(2).path("tool_call_id").asText());
            assertTrue(forwarded.get().path("messages").get(0).path("content").asText()
                    .contains("<contrato_de_execucao>"));
            assertEquals(2, request.path("messages").size());
        } finally {
            local.stop(0);
        }
    }

    @Test
    void localInternalPromptPreservesReferenceAndGivesReasoningAnIndependentTokenBudget() throws IOException {
        AtomicReference<JsonNode> forwarded = new AtomicReference<>();
        HttpServer local = server(exchange -> {
            forwarded.set(mapper.readTree(exchange.getRequestBody()));
            respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"DISCIPLINA I\"}}]}");
        });
        try {
            AiGatewayProperties properties = properties("LOCAL_ONLY", local);
            properties.setLocalBaseUrl(baseUrl(local));
            properties.setLocalModel("qwen-test");
            properties.setLocalInternalMaxTokens(2048);
            properties.setLocalTemperature(0.6);
            properties.setLocalTopP(0.95);
            ObjectNode request = request();
            request.withArray("messages").insert(0, mapper.createObjectNode()
                    .put("role", "system").put("content", "Retorne somente o nome da lista."));
            ((ObjectNode) request.withArray("messages").get(1))
                    .put("content", "Referência: [\"DISCIPLINA I\"] Entrada: disciplina 1");
            request.put("max_tokens", 500);
            request.put("temperature", 0.2);
            request.put("top_p", 0.9);
            request.put("max_completion_tokens", 500);
            ObjectNode original = request.deepCopy();
            new AiGatewayService(properties, mapper, telemetry).complete(request,
                    new AiInferenceContext(AiInferenceContext.Operation.INTERNAL_TEXT, null));
            assertTrue(forwarded.get().path("messages").get(0).path("content").asText()
                    .startsWith("Retorne somente o nome da lista."));
            assertTrue(forwarded.get().path("messages").get(0).path("content").asText()
                    .contains("<contrato_da_tarefa_interna>"));
            assertEquals(original.path("messages").get(1), forwarded.get().path("messages").get(1));
            assertEquals(2048, forwarded.get().path("max_tokens").asInt());
            assertFalse(forwarded.get().has("max_completion_tokens"));
            assertEquals(original, request);

            properties.setMode("EXTERNAL_ONLY");
            properties.setExternalApiKey("test-secret");
            properties.setExternalModel("external-test");
            new AiGatewayService(properties, mapper, telemetry).complete(request,
                    new AiInferenceContext(AiInferenceContext.Operation.INTERNAL_TEXT, null));
            assertEquals(original.path("messages"), forwarded.get().path("messages"));
            assertEquals(500, forwarded.get().path("max_tokens").asInt());
            assertEquals(0.2, forwarded.get().path("temperature").asDouble());
            assertEquals(0.9, forwarded.get().path("top_p").asDouble());
        } finally {
            local.stop(0);
        }
    }

    @Test
    void localStructuredPromptsCanBeDisabledAndJsonOperationsKeepTheirContract() throws IOException {
        AtomicReference<JsonNode> forwarded = new AtomicReference<>();
        HttpServer local = server(exchange -> {
            forwarded.set(mapper.readTree(exchange.getRequestBody()));
            respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}");
        });
        try {
            AiGatewayProperties properties = properties("LOCAL_ONLY", local);
            properties.setLocalBaseUrl(baseUrl(local));
            properties.setLocalModel("qwen-test");
            ObjectNode request = request();
            request.set("response_format", mapper.readTree(
                    "{\"type\":\"json_schema\",\"json_schema\":{\"name\":\"fixture\",\"schema\":{\"type\":\"object\"}}}"));
            new AiGatewayService(properties, mapper, telemetry).complete(request,
                    new AiInferenceContext(AiInferenceContext.Operation.INTERNAL_JSON, null));
            String instructions = forwarded.get().path("messages").get(0).path("content").asText();
            assertTrue(instructions.contains("<contrato_da_resposta_estruturada>"));
            assertFalse(instructions.contains("<contrato_de_execucao>"));
            assertEquals(request.path("response_format"), forwarded.get().path("response_format"));

            properties.setLocalStructuredPrompts(false);
            new AiGatewayService(properties, mapper, telemetry).complete(request);
            assertEquals(request.path("messages"), forwarded.get().path("messages"));
        } finally {
            local.stop(0);
        }
    }

    @Test
    void localOnlyNeverFallsBackToExternalOnProviderFailure() throws IOException {
        AtomicInteger localCalls = new AtomicInteger();
        AtomicInteger externalCalls = new AtomicInteger();
        HttpServer local = server(exchange -> {
            localCalls.incrementAndGet();
            respond(exchange, 500, "{} ");
        });
        HttpServer external = server(exchange -> {
            externalCalls.incrementAndGet();
            respond(exchange, 200, "{}");
        });
        try {
            AiGatewayProperties properties = properties("LOCAL_ONLY", local);
            properties.setLocalModel("local-test");
            properties.setLocalBaseUrl(baseUrl(local));
            properties.setExternalBaseUrl(baseUrl(external));
            properties.setExternalApiKey("external-secret");
            AiGatewayService service = new AiGatewayService(properties, mapper, telemetry);

            AiGatewayException error = assertThrows(AiGatewayException.class,
                    () -> service.complete(request()));

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
            assertEquals(1, localCalls.get());
            assertEquals(0, externalCalls.get());
            org.mockito.ArgumentCaptor<AiInferenceEvent> event = org.mockito.ArgumentCaptor.forClass(AiInferenceEvent.class);
            verify(telemetry).recordSafely(event.capture());
            assertEquals("FAILURE", event.getValue().result());
            assertEquals("BAD_GATEWAY", event.getValue().failureCode());
            assertNull(event.getValue().inputTokens());
        } finally {
            local.stop(0);
            external.stop(0);
        }
    }

    @Test
    void rejectsStreamingAndUnimplementedHybridMode() {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setMode("LOCAL_ONLY");
        properties.setLocalModel("local-test");
        AiGatewayService service = new AiGatewayService(properties, mapper, telemetry);
        ObjectNode request = request();
        request.put("stream", true);
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(AiGatewayException.class, () -> service.complete(request)).getStatus());

        properties.setMode("HYBRID");
        assertThrows(IllegalStateException.class, () -> new AiGatewayService(properties, mapper, telemetry));
    }

    private ObjectNode request() {
        ObjectNode request = mapper.createObjectNode();
        request.set("messages", mapper.createArrayNode().add(
                mapper.createObjectNode().put("role", "user").put("content", "olá")));
        return request;
    }

    private AiGatewayProperties properties(String mode, HttpServer server) {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setMode(mode);
        properties.setExternalBaseUrl(baseUrl(server));
        properties.setTimeout(Duration.ofSeconds(3));
        return properties;
    }

    private String baseUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    private HttpServer server(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", handler);
        server.start();
        return server;
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
