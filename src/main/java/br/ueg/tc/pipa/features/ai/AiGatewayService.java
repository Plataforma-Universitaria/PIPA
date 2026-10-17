package br.ueg.tc.pipa.features.ai;

import br.ueg.tc.pipa.features.observability.AiInferenceEvent;
import br.ueg.tc.pipa.features.observability.AiInferenceTelemetryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

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
    private final AiInferenceTelemetryService telemetry;

    public AiGatewayService(AiGatewayProperties properties, ObjectMapper mapper,
                            AiInferenceTelemetryService telemetry) {
        this.properties = properties;
        this.mapper = mapper;
        this.telemetry = telemetry;
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
        return complete(incoming, new AiInferenceContext(AiInferenceContext.Operation.AGENT_CHAT, null));
    }

    public GatewayResult complete(ObjectNode incoming, AiInferenceContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Contexto de inferência é obrigatório");
        }
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

        String requestId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        log.info("Inferência iniciada: requestId={} turnId={} operation={} provider={} model={} messageCount={} offeredTools={}",
                requestId, context.turnId(), context.operation(), providerName, safeModel(model),
                incoming.path("messages").size(), incoming.path("tools").isArray() ? incoming.path("tools").size() : 0);
        try {
            if (model == null || model.isBlank() || apiKey == null || apiKey.isBlank()) {
                throw new AiGatewayException(HttpStatus.SERVICE_UNAVAILABLE, "Provedor de IA não configurado");
            }
            ObjectNode request = mapper.createObjectNode();
            for (String field : FORWARDED_FIELDS) {
                JsonNode value = incoming.get(field);
                if (value != null) request.set(field, value);
            }
            request.put("model", model);
            if (local && properties.getLocalTemperature() != null) {
                request.put("temperature", properties.getLocalTemperature());
            }
            if (local && properties.getLocalTopP() != null) {
                request.put("top_p", properties.getLocalTopP());
            }
            if (local && !properties.getLocalReasoningEffort().isBlank()) {
                request.put("reasoning_effort", properties.getLocalReasoningEffort());
            }
            if (local && properties.isLocalStructuredPrompts()) {
                ArrayNode messages = request.withArray("messages").deepCopy();
                JsonNode first = messages.get(0);
                String instructions = LocalInferencePrompts.forOperation(context.operation());
                if ("system".equals(first.path("role").asText()) && first.path("content").isTextual()) {
                    ((ObjectNode) first).put("content", first.path("content").asText() + "\n" + instructions);
                } else {
                    messages.insert(0, mapper.createObjectNode().put("role", "system").put("content", instructions));
                }
                request.set("messages", messages);
            }
            if (local && context.operation() != AiInferenceContext.Operation.AGENT_CHAT) {
                if (properties.getLocalInternalMaxTokens() <= 0) {
                    throw new AiGatewayException(HttpStatus.SERVICE_UNAVAILABLE,
                            "AI_LOCAL_INTERNAL_MAX_TOKENS deve ser positivo");
                }
                request.remove("max_completion_tokens");
                request.put("max_tokens", properties.getLocalInternalMaxTokens());
            }
            request.put("stream", false);
            ChatCompletionProvider provider = new OpenAiCompatibleProvider(
                    client, mapper, baseUrl, apiKey, properties.getTimeout());
            JsonNode response = provider.complete(request);
            long durationMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
            JsonNode usage = response.path("usage");
            telemetry.recordSafely(event(requestId, context, providerName, model, durationMs, usage,
                    "SUCCESS", null));
            JsonNode choice = response.path("choices").path(0);
            JsonNode toolCalls = choice.path("message").path("tool_calls");
            log.info("Inferência concluída: requestId={} turnId={} operation={} provider={} model={} durationMs={} inputTokens={} outputTokens={} finishReason={} toolCallCount={} calledTools={}",
                    requestId, context.turnId(), context.operation(), providerName, safeModel(model), durationMs,
                    tokenCount(usage, "prompt_tokens"), tokenCount(usage, "completion_tokens"),
                    safeLogValue(choice.path("finish_reason").asText()),
                    toolCalls.isArray() ? toolCalls.size() : 0, calledToolNames(toolCalls));
            return new GatewayResult(response, providerName, model, requestId);
        } catch (RuntimeException e) {
            String failureCode = e instanceof AiGatewayException gatewayFailure
                    ? gatewayFailure.getStatus().name() : "INTERNAL_ERROR";
            long durationMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
            telemetry.recordSafely(event(requestId, context, providerName, model, durationMs, null,
                    "FAILURE", failureCode));
            log.warn("Inferência falhou: requestId={} turnId={} operation={} provider={} model={} durationMs={} failureCode={} errorType={}",
                    requestId, context.turnId(), context.operation(), providerName, safeModel(model),
                    durationMs, failureCode, e.getClass().getSimpleName());
            throw e;
        }
    }

    private List<String> calledToolNames(JsonNode toolCalls) {
        if (!toolCalls.isArray()) return List.of();
        return StreamSupport.stream(toolCalls.spliterator(), false)
                .limit(20)
                .map(call -> safeLogValue(call.path("function").path("name").asText()))
                .toList();
    }

    private String safeLogValue(String value) {
        if (value == null) return "";
        String sanitized = value.replaceAll("[^A-Za-z0-9_-]", "_");
        return sanitized.substring(0, Math.min(sanitized.length(), 80));
    }

    private AiInferenceEvent event(String requestId, AiInferenceContext context, String provider,
                                   String model, long durationMs, JsonNode usage,
                                   String result, String failureCode) {
        return new AiInferenceEvent(requestId,
                context.turnId() == null ? null : context.turnId().toString(),
                context.operation().name(), mode.name(), "FIXED_MODE", provider,
                safeModel(model),
                durationMs,
                tokenCount(usage, "prompt_tokens"),
                tokenCount(usage, "completion_tokens"),
                tokenCount(usage, "total_tokens"), result, failureCode);
    }

    private Long tokenCount(JsonNode usage, String field) {
        JsonNode count = usage == null ? null : usage.path(field);
        return count != null && count.isIntegralNumber() && count.canConvertToLong()
                && count.longValue() >= 0 ? count.longValue() : null;
    }

    private String safeModel(String model) {
        if (model == null) return null;
        String safe = model.replaceAll("[^A-Za-z0-9_./:-]", "_");
        return safe.substring(0, Math.min(safe.length(), 128));
    }

    public record GatewayResult(JsonNode body, String provider, String model, String requestId) {}

    private enum RoutingMode { EXTERNAL_ONLY, LOCAL_ONLY, HYBRID }
}
