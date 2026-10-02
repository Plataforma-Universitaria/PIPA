package br.ueg.tc.pipa.features.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class AiGatewayService {
    private static final Logger log = LoggerFactory.getLogger(AiGatewayService.class);
    private static final List<String> FORWARDED_FIELDS = List.of(
            "messages", "tools", "tool_choice", "temperature", "top_p", "max_tokens",
            "max_completion_tokens", "parallel_tool_calls", "response_format", "stop", "seed"
    );

    private final AiGatewayProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient client;
    private final RoutingMode mode;

    public AiGatewayService(AiGatewayProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.mode = RoutingMode.valueOf(properties.getMode().trim().toUpperCase());
        if (mode == RoutingMode.HYBRID) {
            throw new IllegalStateException("HYBRID ainda não foi implementado; use LOCAL_ONLY ou EXTERNAL_ONLY");
        }
        if (mode == RoutingMode.LOCAL_ONLY && properties.getLocalModel().isBlank()) {
            throw new IllegalStateException("AI_LOCAL_MODEL é obrigatório em LOCAL_ONLY");
        }
    }

    public GatewayResult complete(ObjectNode incoming) {
        if (incoming == null || !incoming.path("messages").isArray()
                || incoming.path("messages").isEmpty()) {
            throw new AiGatewayException(HttpStatus.BAD_REQUEST, "messages deve ser uma lista não vazia");
        }
        if (incoming.toString().length() > 2_000_000) {
            throw new AiGatewayException(HttpStatus.PAYLOAD_TOO_LARGE, "Requisição de IA excede o limite");
        }
        if (incoming.path("stream").asBoolean(false)) {
            throw new AiGatewayException(HttpStatus.BAD_REQUEST, "Streaming ainda não é suportado pelo gateway");
        }

        boolean local = mode == RoutingMode.LOCAL_ONLY;
        String providerName = local ? "local" : "external";
        String model = local ? properties.getLocalModel() : properties.getExternalModel();
        String apiKey = local ? "ollama" : properties.getExternalApiKey();
        String baseUrl = local ? properties.getLocalBaseUrl() : properties.getExternalBaseUrl();
        if (model == null || model.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new AiGatewayException(HttpStatus.SERVICE_UNAVAILABLE, "Provedor de IA não configurado");
        }

        ObjectNode request = mapper.createObjectNode();
        for (String field : FORWARDED_FIELDS) {
            JsonNode value = incoming.get(field);
            if (value != null) request.set(field, value);
        }
        request.put("model", model);
        request.put("stream", false);

        String requestId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        try {
            ChatCompletionProvider provider = new OpenAiCompatibleProvider(
                    client, mapper, baseUrl, apiKey, properties.getTimeout());
            JsonNode response = provider.complete(request);
            long durationMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
            JsonNode usage = response.path("usage");
            log.info("Inferência concluída: requestId={} provider={} model={} durationMs={} inputTokens={} outputTokens={}",
                    requestId, providerName, model, durationMs,
                    tokenCount(usage, "prompt_tokens"), tokenCount(usage, "completion_tokens"));
            return new GatewayResult(response, providerName, model, requestId);
        } catch (AiGatewayException e) {
            log.warn("Inferência falhou: requestId={} provider={} model={} errorType={}",
                    requestId, providerName, model, e.getStatus().name());
            throw e;
        }
    }

    private Integer tokenCount(JsonNode usage, String field) {
        JsonNode count = usage.path(field);
        return count.isIntegralNumber() ? count.intValue() : null;
    }

    public record GatewayResult(JsonNode body, String provider, String model, String requestId) {}

    private enum RoutingMode { EXTERNAL_ONLY, LOCAL_ONLY, HYBRID }
}
