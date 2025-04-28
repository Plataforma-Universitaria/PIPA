package br.ueg.tc.pipa.controllers;

import br.ueg.tc.apiai.contract.client.AbstractClient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientTest extends AbstractClient {
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Override
    public String getApiKey() {
        return apiKey;
    }

    @Override
    public String getModel() {
        return model;
    }
}
