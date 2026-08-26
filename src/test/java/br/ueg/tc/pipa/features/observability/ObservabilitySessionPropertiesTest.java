package br.ueg.tc.pipa.features.observability;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilitySessionPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void shouldBindConfiguredSessionTtl() {
        contextRunner
                .withPropertyValues("observability.session.ttl=1800s")
                .run(context -> assertThat(context.getBean(ObservabilitySessionProperties.class).getTtl())
                        .isEqualTo(Duration.ofMinutes(30)));
    }

    @Test
    void shouldUseOneHourAsDefaultSessionTtl() {
        contextRunner.run(context -> assertThat(context.getBean(ObservabilitySessionProperties.class).getTtl())
                .isEqualTo(Duration.ofHours(1)));
    }

    @Test
    void shouldRejectNonPositiveSessionTtl() {
        contextRunner
                .withPropertyValues("observability.session.ttl=0s")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ObservabilitySessionProperties.class)
    static class PropertiesConfiguration {
    }
}
