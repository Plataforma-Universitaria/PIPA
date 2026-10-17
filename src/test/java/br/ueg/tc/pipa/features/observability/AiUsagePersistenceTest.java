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
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AiUsagePersistenceTest.JpaTestConfiguration.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai_usage;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
class AiUsagePersistenceTest {
    @Autowired
    private AiInferenceLogRepository repository;

    @Test
    void treatsBlankLegacyOriginAsExternal() {
        persist("", 100L, "2026-10-14T03:00:00Z");
        persist("local", 200L, "2026-10-14T03:00:00Z");
        assertThat(repository.aggregateUsage(null, null, "external").totalTokens()).isEqualTo(100L);
        assertThat(repository.aggregateUsage(null, null, "local").totalTokens()).isEqualTo(200L);
    }

    @Test
    void aggregatesKnownTokensByProviderAndInclusivePeriodWithoutReplacingUnknowns() {
        persist("local", 100L, "2026-10-14T03:00:00Z");
        persist("local", null, "2026-10-14T15:00:00Z");
        persist("external", 900L, "2026-10-15T02:59:59.999Z");
        persist("external", 5000L, "2026-10-15T03:00:00Z");
        Instant from = Instant.parse("2026-10-14T03:00:00Z");
        Instant to = Instant.parse("2026-10-15T02:59:59.999Z");

        var all = repository.aggregateUsage(from, to, null);
        assertThat(all.totalInferences()).isEqualTo(3);
        assertThat(all.inferencesWithTokenUsage()).isEqualTo(2);
        assertThat(all.totalTokens()).isEqualTo(1000L);
        var local = repository.aggregateUsage(from, to, "local");
        assertThat(local.totalInferences()).isEqualTo(2);
        assertThat(local.inferencesWithTokenUsage()).isEqualTo(1);
        assertThat(local.totalTokens()).isEqualTo(100L);
        assertThat(repository.aggregateUsage(from, to, "external").totalTokens()).isEqualTo(900L);
        assertThat(repository.aggregateUsage(null, null, null).totalTokens()).isEqualTo(6000L);
        assertThat(repository.aggregateUsage(from, null, null).totalInferences()).isEqualTo(4);
        assertThat(repository.aggregateUsage(null, to, null).totalInferences()).isEqualTo(3);
        var service = new AiUsageService(repository);
        var scoped = service.getUsage(LocalDateTime.parse("2026-10-14T00:00:00"),
                LocalDateTime.parse("2026-10-14T23:59:59.999"), "external");
        assertThat(scoped.totalTokens()).isEqualTo(900L);
        assertThat(service.getUsage(null, null, " ").totalTokens()).isEqualTo(6000L);
    }

    @Test
    void rejectsUnknownOriginAndInvertedPeriod() {
        var service = new AiUsageService(repository);
        assertThatThrownBy(() -> service.getUsage(null, null, "ueg-provider"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        assertThatThrownBy(() -> service.getUsage(LocalDateTime.parse("2026-10-15T00:00:00"),
                LocalDateTime.parse("2026-10-14T00:00:00"), null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
    }

    @Test
    void distinguishesUnknownTokensFromMeasuredZeroAndEmptyPeriod() {
        var empty = repository.aggregateUsage(null, null, null);
        assertThat(empty.totalInferences()).isZero();
        assertThat(empty.totalTokens()).isNull();
        persist("local", null, "2026-10-14T03:00:00Z");
        var unknown = repository.aggregateUsage(null, null, "local");
        assertThat(unknown.totalInferences()).isEqualTo(1);
        assertThat(unknown.inferencesWithTokenUsage()).isZero();
        assertThat(unknown.totalTokens()).isNull();
        persist("external", 0L, "2026-10-14T03:00:00Z");
        var measured = repository.aggregateUsage(null, null, "external");
        assertThat(measured.inferencesWithTokenUsage()).isEqualTo(1);
        assertThat(measured.totalTokens()).isZero();
    }

    private void persist(String provider, Long tokens, String timestamp) {
        var event = new AiInferenceEvent(UUID.randomUUID().toString(), null, "AGENT_CHAT",
                "local".equals(provider) ? "LOCAL_ONLY" : "EXTERNAL_ONLY", "FIXED_MODE",
                provider, "model", 125L, null, null, tokens, "SUCCESS", null);
        var log = new AiInferenceLog(event);
        org.springframework.test.util.ReflectionTestUtils.setField(log, "timestamp", Instant.parse(timestamp));
        repository.saveAndFlush(log);
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
