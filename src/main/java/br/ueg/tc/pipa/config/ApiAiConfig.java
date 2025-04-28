package br.ueg.tc.pipa.config;

import br.ueg.tc.apiai.contract.client.ChatClientFactory;
import br.ueg.tc.apiai.service.AiService;
import br.ueg.tc.pipa.controllers.ClientTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiAiConfig {
    @Bean
    public ChatClientFactory<ClientTest> chatClientFactory(ClientTest client) {
        return new ChatClientFactory<>(client);
    }

    @Bean
    public AiService aiService(ChatClientFactory<ClientTest> chatClientFactory) {
        return new AiService(chatClientFactory);
    }
}