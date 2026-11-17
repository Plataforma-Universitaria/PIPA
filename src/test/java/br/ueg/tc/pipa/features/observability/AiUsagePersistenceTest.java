package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.institution.Institution;
import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLog;
import jakarta.persistence.EntityManager;
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

    @Autowired
    private EntityManager entityManager;

    @Test
    void filtersWholeTurnsWithoutMultiplyingTokensAndRequiresOneMatchingExecution() {
        User ueg = user("UEG");
        User other = user("UFG");
        String firstTurn = UUID.randomUUID().toString();
        String secondTurn = UUID.randomUUID().toString();
        String otherTurn = UUID.randomUUID().toString();
        for (long tokens : new long[]{100, 50, 150}) persistTurn(firstTurn, tokens);
        persistTurn(secondTurn, 200);
        persistTurn(otherTurn, 300);
        persistTurn(null, 400);
        persistTurn(UUID.randomUUID().toString(), 500);
        tool(firstTurn, "consultar_notas", "Aluno", ueg);
        tool(firstTurn, "consultar_notas", "Aluno", ueg);
        tool(firstTurn, "consultar_horario", "Professor", ueg);
        tool(secondTurn, "consultar_notas", "Professor", ueg);
        tool(otherTurn, "consultar_notas", "Aluno", other);
        tool(null, "consultar_notas", "Aluno", ueg);
        entityManager.flush();

        var service = new AiUsageService(repository);
        var selected = service.getUsage(null, null, "external", " AlUnO ", " uEg ", " CONSULTAR_NOTAS ");
        assertThat(selected.totalInferences()).isEqualTo(3);
        assertThat(selected.totalTokens()).isEqualTo(300L);
        assertThat(selected.inputTokens()).isEqualTo(300L);
        assertThat(selected.outputTokens()).isZero();
        assertThat(selected.inferencesWithTokenUsage()).isEqualTo(3);
        assertThat(service.getUsage(null, null, null, "Aluno", null, null).totalTokens()).isEqualTo(600L);
        assertThat(service.getUsage(null, null, null, null, "UEG", null).totalTokens()).isEqualTo(500L);
        assertThat(service.getUsage(null, null, null, null, null, "consultar_notas").totalTokens()).isEqualTo(800L);
        assertThat(service.getUsage(null, null, null, "Professor", "UEG", "consultar_notas").totalTokens()).isEqualTo(200L);
        assertThat(service.getUsage(null, null, "local", "Aluno", "UEG", "consultar_notas").totalInferences()).isZero();
        assertThat(service.getUsage(null, null, null, " ", "", null).totalTokens()).isEqualTo(1700L);
    }

    private User user(String name) {
        var institution = new Institution();
        institution.setShortName(name);
        institution.setProviderClass(name + "Provider");
        institution.setProviderPath(name.toLowerCase());
        entityManager.persist(institution);
        var user = new User();
        user.setExternalKey(UUID.randomUUID());
        user.setInstitution(institution);
        entityManager.persist(user);
        return user;
    }

    private void tool(String turnId, String name, String persona, User user) {
        var tool = new ToolExecutionLog();
        tool.setTurnId(turnId);
        tool.setToolName(name);
        tool.setPersona(persona);
        tool.setUser(user);
        tool.setTimestamp(LocalDateTime.of(2026, 10, 14, 12, 0));
        entityManager.persist(tool);
    }

    private void persistTurn(String turnId, long tokens) {
        repository.saveAndFlush(new AiInferenceLog(new AiInferenceEvent(UUID.randomUUID().toString(),
                turnId, "AGENT_CHAT", "EXTERNAL_ONLY", "FIXED_MODE", "external", "model",
                100L, tokens, 0L, tokens, "SUCCESS", null)));
    }

    @Test
    void treatsBlankLegacyOriginAsExternal() {
        persist("", 100L, "2026-10-14T03:00:00Z");
        persist("local", 200L, "2026-10-14T03:00:00Z");
        assertThat(repository.aggregateUsage(null, null, "external").totalTokens()).isEqualTo(100L);
        assertThat(repository.aggregateUsage(null, null, "local").totalTokens()).isEqualTo(200L);
    }

    @Test
    void aggregatesKnownTokensByProviderAndInclusivePeriodWithoutReplacingUnknowns() {
        persist("local", 80L, 20L, 100L, "2026-10-14T03:00:00Z");
        persist("local", null, 0L, null, "2026-10-14T15:00:00Z");
        persist("external", 700L, 200L, 900L, "2026-10-15T02:59:59.999Z");
        persist("external", 4000L, 1000L, 5000L, "2026-10-15T03:00:00Z");
        Instant from = Instant.parse("2026-10-14T03:00:00Z");
        Instant to = Instant.parse("2026-10-15T02:59:59.999Z");

        var all = repository.aggregateUsage(from, to, null);
        assertThat(all.totalInferences()).isEqualTo(3);
        assertThat(all.inferencesWithTokenUsage()).isEqualTo(2);
        assertThat(all.totalTokens()).isEqualTo(1000L);
        assertThat(all.inputTokens()).isEqualTo(780L);
        assertThat(all.outputTokens()).isEqualTo(220L);
        assertThat(all.inferencesWithInputTokens()).isEqualTo(2);
        assertThat(all.inferencesWithOutputTokens()).isEqualTo(3);
        var local = repository.aggregateUsage(from, to, "local");
        assertThat(local.totalInferences()).isEqualTo(2);
        assertThat(local.inferencesWithTokenUsage()).isEqualTo(1);
        assertThat(local.totalTokens()).isEqualTo(100L);
        assertThat(local.inputTokens()).isEqualTo(80L);
        assertThat(local.outputTokens()).isEqualTo(20L);
        assertThat(local.inferencesWithInputTokens()).isEqualTo(1);
        assertThat(local.inferencesWithOutputTokens()).isEqualTo(2);
        assertThat(repository.aggregateUsage(from, to, "external").totalTokens()).isEqualTo(900L);
        assertThat(repository.aggregateUsage(null, null, null).totalTokens()).isEqualTo(6000L);
        assertThat(repository.aggregateUsage(from, null, null).totalInferences()).isEqualTo(4);
        assertThat(repository.aggregateUsage(null, to, null).totalInferences()).isEqualTo(3);
        var service = new AiUsageService(repository);
        var scoped = service.getUsage(LocalDateTime.parse("2026-10-14T00:00:00"),
                LocalDateTime.parse("2026-10-14T23:59:59.999"), "external");
        assertThat(scoped.totalTokens()).isEqualTo(900L);
        assertThat(scoped.inputTokens()).isEqualTo(700L);
        assertThat(scoped.outputTokens()).isEqualTo(200L);
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
        assertThat(empty.inputTokens()).isNull();
        assertThat(empty.outputTokens()).isNull();
        assertThat(empty.inferencesWithInputTokens()).isZero();
        assertThat(empty.inferencesWithOutputTokens()).isZero();
        persist("local", null, "2026-10-14T03:00:00Z");
        var unknown = repository.aggregateUsage(null, null, "local");
        assertThat(unknown.totalInferences()).isEqualTo(1);
        assertThat(unknown.inferencesWithTokenUsage()).isZero();
        assertThat(unknown.totalTokens()).isNull();
        assertThat(unknown.inputTokens()).isNull();
        assertThat(unknown.outputTokens()).isNull();
        assertThat(unknown.inferencesWithInputTokens()).isZero();
        assertThat(unknown.inferencesWithOutputTokens()).isZero();
        persist("external", 0L, 0L, 0L, "2026-10-14T03:00:00Z");
        var measured = repository.aggregateUsage(null, null, "external");
        assertThat(measured.inferencesWithTokenUsage()).isEqualTo(1);
        assertThat(measured.totalTokens()).isZero();
        assertThat(measured.inputTokens()).isZero();
        assertThat(measured.outputTokens()).isZero();
        assertThat(measured.inferencesWithInputTokens()).isEqualTo(1);
        assertThat(measured.inferencesWithOutputTokens()).isEqualTo(1);
    }

    @Test
    void aggregatesEachTokenFieldIndependentlyWhenTotalIsUnknown() {
        persist("local", 50L, null, null, "2026-10-14T03:00:00Z");
        persist("local", null, 10L, null, "2026-10-14T04:00:00Z");

        var usage = repository.aggregateUsage(null, null, "local");
        assertThat(usage.totalInferences()).isEqualTo(2);
        assertThat(usage.totalTokens()).isNull();
        assertThat(usage.inferencesWithTokenUsage()).isZero();
        assertThat(usage.inputTokens()).isEqualTo(50L);
        assertThat(usage.outputTokens()).isEqualTo(10L);
        assertThat(usage.inferencesWithInputTokens()).isEqualTo(1);
        assertThat(usage.inferencesWithOutputTokens()).isEqualTo(1);
    }

    private void persist(String provider, Long tokens, String timestamp) {
        persist(provider, null, null, tokens, timestamp);
    }

    private void persist(String provider, Long inputTokens, Long outputTokens, Long tokens, String timestamp) {
        var event = new AiInferenceEvent(UUID.randomUUID().toString(), null, "AGENT_CHAT",
                "local".equals(provider) ? "LOCAL_ONLY" : "EXTERNAL_ONLY", "FIXED_MODE",
                provider, "model", 125L, inputTokens, outputTokens, tokens, "SUCCESS", null);
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
    @EntityScan(basePackages = {"br.ueg.tc.pipa.features.observability", "br.ueg.tc.pipa.domain"})
    @EnableJpaRepositories(basePackageClasses = AiInferenceLogRepository.class)
    static class JpaTestConfiguration {}
}
