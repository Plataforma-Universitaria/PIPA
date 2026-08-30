package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.institution.Institution;
import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLog;
import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLogRepository;
import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.usersession.UserSession;
import br.ueg.tc.pipa.domain.usersession.UserSessionRepository;
import br.ueg.tc.pipa.domain.user.UserRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

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

    @Autowired
    private EntityManager entityManager;

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

    @Test
    void shouldAccumulateEveryProvidedSpecificationFilter() {
        Institution institution = persistInstitution("UEG", "br.ueg.provider", "ueg-provider");
        User user = persistUser(institution);
        UserSession session = persistSession(user, "TELEGRAM", "chat-123");

        ToolExecutionLog matching = baseLog("consultar_notas");
        matching.setUser(user);
        matching.setUserSession(session);
        matching.setSessionId("chat-123");
        matching.setPersona("Aluno");
        matching.setResult("Sucesso");
        matching.setTimestamp(LocalDateTime.of(2026, 8, 25, 12, 0));
        toolExecutionLogRepository.save(matching);

        ToolExecutionLog other = baseLog("consultar_notas");
        other.setUser(user);
        other.setUserSession(session);
        other.setSessionId("chat-123");
        other.setPersona("Professor");
        other.setResult("Sucesso");
        other.setTimestamp(LocalDateTime.of(2026, 8, 25, 12, 0));
        toolExecutionLogRepository.saveAndFlush(other);

        ObservabilityFilter filter = new ObservabilityFilter(
                LocalDateTime.of(2026, 8, 25, 11, 0),
                LocalDateTime.of(2026, 8, 25, 13, 0),
                "aluno", "CONSULTAR_NOTAS", "ueg", "UEG-PROVIDER",
                "telegram", "sucesso", session.getId(), "CHAT-123"
        );

        Page<ToolExecutionLog> result = toolExecutionLogRepository.findAll(
                ToolExecutionLogSpecification.from(filter),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp"))
        );

        assertThat(result.getContent()).containsExactly(matching);
    }

    @Test
    void shouldApplyTemporalLimitsIndependentlyAndInclusively() {
        ToolExecutionLog first = baseLog("primeira");
        first.setTimestamp(LocalDateTime.of(2026, 8, 25, 10, 0));
        toolExecutionLogRepository.save(first);
        ToolExecutionLog second = baseLog("segunda");
        second.setTimestamp(LocalDateTime.of(2026, 8, 25, 11, 0));
        toolExecutionLogRepository.saveAndFlush(second);

        ObservabilityFilter onlyFrom = new ObservabilityFilter(
                second.getTimestamp(), null, null, null, null,
                null, null, null, null, null
        );
        ObservabilityFilter onlyTo = new ObservabilityFilter(
                null, first.getTimestamp(), null, null, null,
                null, null, null, null, null
        );

        assertThat(toolExecutionLogRepository.findAll(ToolExecutionLogSpecification.from(onlyFrom)))
                .containsExactly(second);
        assertThat(toolExecutionLogRepository.findAll(ToolExecutionLogSpecification.from(onlyTo)))
                .containsExactly(first);
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

    private Institution persistInstitution(String shortName, String providerClass, String providerPath) {
        Institution institution = new Institution();
        institution.setShortName(shortName);
        institution.setProviderClass(providerClass);
        institution.setProviderPath(providerPath);
        entityManager.persist(institution);
        return institution;
    }

    private User persistUser(Institution institution) {
        User user = new User();
        user.setExternalKey(UUID.randomUUID());
        user.setInstitution(institution);
        user.setPersonas(new ArrayList<>(java.util.List.of("Aluno")));
        entityManager.persist(user);
        return user;
    }

    private UserSession persistSession(User user, String channel, String fingerprint) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setChannel(channel);
        session.setFingerprint(fingerprint);
        return userSessionRepository.saveAndFlush(session);
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            TransactionAutoConfiguration.class
    })
    @EntityScan(basePackages = "br.ueg.tc.pipa.domain")
    @EnableJpaRepositories(basePackageClasses = {
            UserSessionRepository.class,
            ToolExecutionLogRepository.class,
            UserRepository.class
    })
    static class JpaTestConfiguration {
    }
}
