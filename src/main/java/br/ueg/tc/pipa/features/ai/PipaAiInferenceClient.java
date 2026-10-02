package br.ueg.tc.pipa.features.ai;

import br.ueg.tc.pipa_integrator.ai.AiInferenceClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PipaAiInferenceClient implements AiInferenceClient {
    private static final String DEFAULT_SYSTEM_MESSAGE = """
            Você é um assistente de IA especialista e pragmático.
            Sua função é fornecer respostas diretas, factuais e concisas.
            - NÃO use saudações, despedidas ou frases de preenchimento.
            - NÃO dê opiniões, especulações ou conselhos.
            - NÃO explique o que você faz.
            - Foque exclusivamente nos dados e na tarefa solicitada.
            - Responda de forma objetiva e sem rodeios.
            """;

    private final AiGatewayService gateway;
    private final ObjectMapper mapper;

    public PipaAiInferenceClient(AiGatewayService gateway, ObjectMapper mapper) {
        this.gateway = gateway;
        this.mapper = mapper;
    }

    @Override
    public String complete(String prompt) {
        return send(prompt, DEFAULT_SYSTEM_MESSAGE, null);
    }

    @Override
    public String completeJson(String prompt, String systemMessage, String jsonSchema) {
        if (jsonSchema == null || jsonSchema.isBlank()) {
            throw new IllegalArgumentException("jsonSchema é obrigatório");
        }
        return send(prompt, systemMessage, jsonSchema);
    }

    private String send(String prompt, String systemMessage, String jsonSchema) {
        if (prompt == null || prompt.isBlank() || systemMessage == null || systemMessage.isBlank()) {
            throw new IllegalArgumentException("Prompt e instrução de sistema são obrigatórios");
        }

        ObjectNode request = mapper.createObjectNode();
        ArrayNode messages = request.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemMessage);
        messages.addObject().put("role", "user").put("content", prompt);
        request.put("temperature", 0.2);
        request.put("top_p", 0.9);
        request.put("max_tokens", 500);

        if (jsonSchema != null) {
            ObjectNode format = request.putObject("response_format");
            format.put("type", "json_schema");
            ObjectNode schema = format.putObject("json_schema");
            schema.put("name", "ai_execution_plan");
            try {
                schema.set("schema", mapper.readTree(jsonSchema));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Esquema JSON inválido", e);
            }
        }

        JsonNode choice = gateway.complete(request).body().path("choices").path(0);
        String finishReason = choice.path("finish_reason").asText();
        JsonNode content = choice.path("message").path("content");
        if (!content.isTextual() || content.asText().isBlank() || "length".equals(finishReason)) {
            throw new AiGatewayException(HttpStatus.BAD_GATEWAY, "Resposta textual incompleta do provedor de IA");
        }
        return content.asText();
    }
}
