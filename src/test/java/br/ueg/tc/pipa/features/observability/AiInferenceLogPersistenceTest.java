package br.ueg.tc.pipa.features.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AiInferenceLogPersistenceTest.JpaTestConfiguration.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai_inference_log;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                TransactionalTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
class AiInferenceLogPersistenceTest {
    @Autowired
    private AiInferenceLogRepository repository;

    @Test
    void persistsOnlyMetadataAndPreservesUnknownTokenCounts() {
        String requestId = UUID.randomUUID().toString();
        String turnId = UUID.randomUUID().toString();
        AiInferenceEvent event = new AiInferenceEvent(requestId, turnId, "AGENT_CHAT",
                "LOCAL_ONLY", "FIXED_MODE", "local", "qwen3:4b", 125L,
                null, null, null, "SUCCESS", null);

        AiInferenceLog saved = repository.saveAndFlush(new AiInferenceLog(event));
        AiInferenceLog loaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getRequestId()).isEqualTo(requestId);
        assertThat(loaded.getTurnId()).isEqualTo(turnId);
        assertThat(loaded.getModel()).isEqualTo("qwen3:4b");
        assertThat(loaded.getInputTokens()).isNull();
        assertThat(loaded.getOutputTokens()).isNull();
        assertThat(loaded.getTotalTokens()).isNull();
        assertThat(loaded.getTimestamp()).isNotNull();
        assertThat(repository.deleteByTimestampBefore(Instant.now().plusSeconds(1))).isEqualTo(1);
        assertThat(repository.count()).isZero();
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            TransactionAutoConfiguration.class
    })
    @EntityScan(basePackageClasses = AiInferenceLog.class)
    @EnableJpaRepositories(basePackageClasses = AiInferenceLogRepository.class)
    static class JpaTestConfiguration {}
}
