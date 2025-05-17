package br.ueg.tc.pipa.config.ai;

import br.ueg.tc.apiai.contract.client.ChatClientFactory;
import br.ueg.tc.apiai.service.AiService;
import br.ueg.tc.pipa.domain.ai.AIClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiAiConfig {
    @Bean
    public ChatClientFactory<AIClient> chatClientFactory(AIClient client) {
        return new ChatClientFactory<>(client);
    }

    @Bean
    public AiService aiService(ChatClientFactory<AIClient> chatClientFactory) {
        return new AiService(chatClientFactory);
    }
}