package br.ueg.tc.pipa.features.ai;

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

class AiGatewayServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

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
            AiGatewayService service = new AiGatewayService(properties, mapper);
            ObjectNode request = request();
            request.put("model", "modelo-escolhido-pelo-cliente");
            request.set("tools", mapper.readTree("[{\"type\":\"function\",\"function\":{\"name\":\"consultar\",\"parameters\":{\"type\":\"object\"}}}]"));

            AiGatewayService.GatewayResult result = service.complete(request);

            assertEquals("external", result.provider());
            assertEquals("Bearer test-secret", authorization.get());
            assertEquals("external-test", forwarded.get().path("model").asText());
            assertEquals(false, forwarded.get().path("stream").asBoolean());
            assertEquals("consultar", forwarded.get().path("tools").get(0).path("function").path("name").asText());
            assertEquals("call-1", result.body().path("choices").get(0)
                    .path("message").path("tool_calls").get(0).path("id").asText());
        } finally {
            server.stop(0);
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
            AiGatewayService service = new AiGatewayService(properties, mapper);

            AiGatewayException error = assertThrows(AiGatewayException.class,
                    () -> service.complete(request()));

            assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
            assertEquals(1, localCalls.get());
            assertEquals(0, externalCalls.get());
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
        AiGatewayService service = new AiGatewayService(properties, mapper);
        ObjectNode request = request();
        request.put("stream", true);
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(AiGatewayException.class, () -> service.complete(request)).getStatus());

        properties.setMode("HYBRID");
        assertThrows(IllegalStateException.class, () -> new AiGatewayService(properties, mapper));
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
