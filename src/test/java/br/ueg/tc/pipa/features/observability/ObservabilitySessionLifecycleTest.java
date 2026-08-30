package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLogRepository;
import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.user.UserRepository;
import br.ueg.tc.pipa.domain.usersession.UserSession;
import br.ueg.tc.pipa.domain.usersession.UserSessionRepository;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilitySessionLifecycleTest {

    private SessionRepositoryHandler sessions;
    private UserRepositoryHandler users;
    private ObservabilityService service;
    private User user;

    @BeforeEach
    void setUp() {
        sessions = new SessionRepositoryHandler();
        users = new UserRepositoryHandler();
        ObservabilitySessionProperties properties = new ObservabilitySessionProperties();
        properties.setTtl(Duration.ofHours(1));
        service = new ObservabilityService(
                repositoryProxy(UserSessionRepository.class, sessions),
                repositoryProxy(ToolExecutionLogRepository.class, unsupportedRepository()),
                new ObservabilityLogMapper(),
                new SensitiveDataRedactor(),
                new ProviderFailureResolver(),
                repositoryProxy(UserRepository.class, users),
                properties
        );
        user = new User();
        user.setId(7L);
        users.user = user;
    }

    @Test
    void shouldRefreshActivityInsideTtlUsingUserLock() {
        UserSession active = session(LocalDateTime.now().minusMinutes(10));
        LocalDateTime previousActivity = active.getLastActivityAt();
        sessions.active = List.of(active);

        UserSession result = service.startSession(user, "telegram-123", "telegram");

        assertThat(result).isSameAs(active);
        assertThat(result.getLastActivityAt()).isAfter(previousActivity);
        assertThat(result.getEndedAt()).isNull();
        assertThat(users.lockRequests).isEqualTo(1);
        assertThat(sessions.lastChannel).isEqualTo("TELEGRAM");
    }

    @Test
    void shouldCloseExpiredSessionAtTtlBoundaryAndCreateAnother() {
        UserSession expired = session(LocalDateTime.now().minusHours(2));
        LocalDateTime expectedEnd = expired.getLastActivityAt().plusHours(1);
        sessions.active = List.of(expired);

        UserSession result = service.startSession(user, "telegram-123", "TELEGRAM");

        assertThat(expired.getEndedAt()).isEqualTo(expectedEnd);
        assertThat(result).isNotSameAs(expired);
        assertThat(result.getStartedAt()).isEqualTo(result.getLastActivityAt());
        assertThat(result.getEndedAt()).isNull();
    }

    @Test
    void shouldCloseInactiveSessionsAtLastActivityPlusTtl() {
        UserSession inactive = session(LocalDateTime.now().minusHours(3));
        LocalDateTime expectedEnd = inactive.getLastActivityAt().plusHours(1);
        sessions.inactive = List.of(inactive);

        int closed = service.closeInactiveSessions();

        assertThat(closed).isEqualTo(1);
        assertThat(inactive.getEndedAt()).isEqualTo(expectedEnd);
        assertThat(sessions.savedAll).containsExactly(inactive);
    }

    @Test
    void repositoryContractsShouldUsePessimisticWriteLocks() throws Exception {
        Method userLock = UserRepository.class.getMethod("findByIdForUpdate", Long.class);
        Method sessionLock = UserSessionRepository.class.getMethod(
                "findActiveForUpdate", Long.class, String.class, String.class);
        Method schedulerLock = UserSessionRepository.class.getMethod(
                "findInactiveForUpdate", LocalDateTime.class);

        assertThat(userLock.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(sessionLock.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(schedulerLock.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private UserSession session(LocalDateTime lastActivityAt) {
        UserSession session = new UserSession();
        session.setId(11L);
        session.setUser(user);
        session.setFingerprint("telegram-123");
        session.setChannel("TELEGRAM");
        session.setStartedAt(lastActivityAt.minusMinutes(5));
        session.setLastActivityAt(lastActivityAt);
        return session;
    }

    private InvocationHandler unsupportedRepository() {
        return (proxy, method, args) -> {
            throw new UnsupportedOperationException(method.getName());
        };
    }

    @SuppressWarnings("unchecked")
    private <T> T repositoryProxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static class UserRepositoryHandler implements InvocationHandler {
        private User user;
        private int lockRequests;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("findByIdForUpdate")) {
                lockRequests++;
                return Optional.ofNullable(user);
            }
            throw new UnsupportedOperationException(method.getName());
        }
    }

    private static class SessionRepositoryHandler implements InvocationHandler {
        private List<UserSession> active = List.of();
        private List<UserSession> inactive = List.of();
        private List<UserSession> savedAll = List.of();
        private String lastChannel;

        @SuppressWarnings("unchecked")
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findActiveForUpdate" -> {
                    lastChannel = (String) args[2];
                    yield active;
                }
                case "findInactiveForUpdate" -> inactive;
                case "save" -> args[0];
                case "saveAll" -> {
                    savedAll = (List<UserSession>) args[0];
                    yield savedAll;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }
    }
}
