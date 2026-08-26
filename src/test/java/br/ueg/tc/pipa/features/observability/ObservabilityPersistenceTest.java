package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLog;
import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLogRepository;
import br.ueg.tc.pipa.domain.usersession.UserSession;
import br.ueg.tc.pipa.domain.usersession.UserSessionRepository;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureCategory;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ObservabilityPersistenceTest.JpaTestConfiguration.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:observability;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
class ObservabilityPersistenceTest {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private ToolExecutionLogRepository toolExecutionLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistDurationAndSessionAssociation() {
        UserSession session = new UserSession();
        session.setFingerprint("telegram-123");
        session.setChannel("TELEGRAM");
        session = userSessionRepository.saveAndFlush(session);

        ToolExecutionLog log = baseLog("consultar_notas");
        log.setSessionId("telegram-123");
        log.setUserSession(session);
        log.setDurationMs(245L);
        log.setFailureCode("INSTITUTION_COMMUNICATION_ERROR");
        log.setFailureCategory(ProviderFailureCategory.COMMUNICATION);
        log.setFailureStage(ProviderFailureStage.PROVIDER_CALL);
        log.setRetryable(true);
        log = toolExecutionLogRepository.saveAndFlush(log);

        ToolExecutionLog persisted = toolExecutionLogRepository.findById(log.getId()).orElseThrow();
        assertThat(persisted.getDurationMs()).isEqualTo(245L);
        assertThat(persisted.getFailureCode()).isEqualTo("INSTITUTION_COMMUNICATION_ERROR");
        assertThat(persisted.getFailureCategory()).isEqualTo(ProviderFailureCategory.COMMUNICATION);
        assertThat(persisted.getFailureStage()).isEqualTo(ProviderFailureStage.PROVIDER_CALL);
        assertThat(persisted.getRetryable()).isTrue();
        assertThat(persisted.getUserSession().getId()).isEqualTo(session.getId());
        assertThat(toolExecutionLogRepository.findByUserSessionId(session.getId())).containsExactly(persisted);
        assertThat(session.getStartedAt()).isNotNull();
        assertThat(session.getLastActivityAt()).isEqualTo(session.getStartedAt());
    }

    @Test
    void shouldKeepNewColumnsNullableForLegacyRows() {
        ToolExecutionLog legacyLog = toolExecutionLogRepository.saveAndFlush(baseLog("ajuda"));

        assertThat(legacyLog.getDurationMs()).isNull();
        assertThat(legacyLog.getUserSession()).isNull();
        assertThat(legacyLog.getFailureCode()).isNull();
        assertThat(legacyLog.getFailureCategory()).isNull();
        assertThat(legacyLog.getFailureStage()).isNull();
        assertThat(legacyLog.getRetryable()).isNull();

        Integer nullableLastActivity = jdbcTemplate.queryForObject(
                """
                select count(*)
                  from information_schema.columns
                 where upper(table_name) = 'USER_SESSION'
                   and upper(column_name) = 'LAST_ACTIVITY_AT'
                   and is_nullable = 'YES'
                """,
                Integer.class
        );
        assertThat(nullableLastActivity).isEqualTo(1);
    }

    private ToolExecutionLog baseLog(String toolName) {
        ToolExecutionLog log = new ToolExecutionLog();
        log.setToolName(toolName);
        log.setToolVersion("1.0");
        log.setPersona("Aluno");
        log.setResult("Sucesso");
        log.setTimestamp(LocalDateTime.of(2026, 8, 25, 12, 0));
        return log;
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            TransactionAutoConfiguration.class
    })
    @EntityScan(basePackages = "br.ueg.tc.pipa.domain")
    @EnableJpaRepositories(basePackageClasses = {UserSessionRepository.class, ToolExecutionLogRepository.class})
    static class JpaTestConfiguration {
    }
}
