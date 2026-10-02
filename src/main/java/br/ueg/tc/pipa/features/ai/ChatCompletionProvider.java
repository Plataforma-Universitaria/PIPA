package br.ueg.tc.pipa.features.ai;

import com.fasterxml.jackson.databind.JsonNode;

public interface ChatCompletionProvider {
    JsonNode complete(JsonNode request);
}
