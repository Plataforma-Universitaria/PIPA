package br.ueg.tc.pipa.features.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiAutoConfigurationDisabledTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    OpenAiChatAutoConfiguration.class,
                    OpenAiEmbeddingAutoConfiguration.class,
                    OpenAiImageAutoConfiguration.class,
                    OpenAiAudioTranscriptionAutoConfiguration.class,
                    OpenAiAudioSpeechAutoConfiguration.class,
                    OpenAiModerationAutoConfiguration.class));

    @Test
    void localConfigurationStartsWithoutOpenAiKey() {
        assertModelsDisabled("application.properties");
    }

    @Test
    void dockerConfigurationStartsWithoutOpenAiKey() {
        assertModelsDisabled("application-docker.properties");
    }

    private void assertModelsDisabled(String resourceName) {
        contextRunner.withInitializer(context -> {
            try {
                context.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource(
                        resourceName, PropertiesLoaderUtils.loadProperties(new ClassPathResource(resourceName))));
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Configuração de teste indisponível", e);
            }
        }).run(context -> {
            assertThat(context).hasNotFailed();
            for (String beanName : new String[] {
                    "openAiChatModel", "openAiEmbeddingModel", "openAiImageModel",
                    "openAiAudioTranscriptionModel", "openAiAudioSpeechModel", "openAiModerationModel"
            }) {
                assertThat(context.containsBean(beanName)).as(beanName).isFalse();
            }
        });
    }
}
