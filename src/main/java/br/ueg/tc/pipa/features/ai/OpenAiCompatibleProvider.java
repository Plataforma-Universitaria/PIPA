package br.ueg.tc.pipa.features.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Transporta o subconjunto de chat completions usado pelo agente; não executa ferramentas. */
public class OpenAiCompatibleProvider implements ChatCompletionProvider {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final URI endpoint;
    private final String apiKey;
    private final Duration timeout;

    public OpenAiCompatibleProvider(HttpClient client, ObjectMapper mapper, String baseUrl,
                                    String apiKey, Duration timeout) {
        this.client = client;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.timeout = timeout;
        URI base = URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
        if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                || base.getHost() == null || base.getUserInfo() != null) {
            throw new IllegalArgumentException("URL do provedor de IA inválida");
        }
        this.endpoint = base.resolve("chat/completions");
    }

    @Override
    public JsonNode complete(JsonNode request) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(request)))
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiGatewayException(HttpStatus.BAD_GATEWAY,
                        "O provedor de IA recusou a inferência (HTTP " + response.statusCode() + ")");
            }
            JsonNode body = mapper.readTree(response.body());
            if (!body.path("choices").isArray() || body.path("choices").isEmpty()) {
                throw new AiGatewayException(HttpStatus.BAD_GATEWAY, "Resposta inválida do provedor de IA");
            }
            return body;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new AiGatewayException(HttpStatus.GATEWAY_TIMEOUT, "Tempo de resposta do provedor de IA excedido");
        } catch (IOException e) {
            throw new AiGatewayException(HttpStatus.BAD_GATEWAY, "Falha de comunicação com o provedor de IA");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiGatewayException(HttpStatus.SERVICE_UNAVAILABLE, "Inferência interrompida");
        }
    }
}
