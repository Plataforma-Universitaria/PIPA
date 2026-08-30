package br.ueg.tc.pipa.features.observability;

import br.ueg.tc.pipa.domain.institution.Institution;
import br.ueg.tc.pipa.domain.logs.toolexecution.ToolExecutionLog;
import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.usersession.UserSession;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Locale;

public final class ToolExecutionLogSpecification {

    private ToolExecutionLogSpecification() {
    }

    public static Specification<ToolExecutionLog> from(ObservabilityFilter filter) {
        if (filter == null) {
            return unrestricted();
        }
        return occurredFrom(filter.from())
                .and(occurredTo(filter.to()))
                .and(hasPersona(filter.persona()))
                .and(hasToolName(filter.toolName()))
                .and(hasInstitution(filter.institution()))
                .and(hasProvider(filter.provider()))
                .and(hasChannel(filter.channel()))
                .and(hasResult(filter.result()))
                .and(hasUserSessionId(filter.userSessionId()))
                .and(hasLegacySessionId(filter.sessionId()));
    }

    public static Specification<ToolExecutionLog> occurredFrom(LocalDateTime from) {
        if (from == null) return unrestricted();
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("timestamp"), from);
    }

    public static Specification<ToolExecutionLog> occurredTo(LocalDateTime to) {
        if (to == null) return unrestricted();
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("timestamp"), to);
    }

    public static Specification<ToolExecutionLog> hasPersona(String persona) {
        return textEquals("persona", persona);
    }

    public static Specification<ToolExecutionLog> hasToolName(String toolName) {
        return textEquals("toolName", toolName);
    }

    public static Specification<ToolExecutionLog> hasInstitution(String institution) {
        if (!hasText(institution)) return unrestricted();
        return (root, query, builder) -> {
            Join<ToolExecutionLog, User> user = root.join("user", JoinType.LEFT);
            Join<User, Institution> institutionJoin = user.join("institution", JoinType.LEFT);
            return builder.equal(builder.lower(institutionJoin.get("shortName")), normalize(institution));
        };
    }

    public static Specification<ToolExecutionLog> hasProvider(String provider) {
        if (!hasText(provider)) return unrestricted();
        return (root, query, builder) -> {
            Join<ToolExecutionLog, User> user = root.join("user", JoinType.LEFT);
            Join<User, Institution> institution = user.join("institution", JoinType.LEFT);
            return builder.equal(builder.lower(institution.get("providerPath")), normalize(provider));
        };
    }

    public static Specification<ToolExecutionLog> hasChannel(String channel) {
        if (!hasText(channel)) return unrestricted();
        return (root, query, builder) -> {
            Join<ToolExecutionLog, UserSession> session = root.join("userSession", JoinType.LEFT);
            return builder.equal(builder.lower(session.get("channel")), normalize(channel));
        };
    }

    public static Specification<ToolExecutionLog> hasResult(String result) {
        return textEquals("result", result);
    }

    public static Specification<ToolExecutionLog> hasUserSessionId(Long userSessionId) {
        if (userSessionId == null) return unrestricted();
        return (root, query, builder) -> builder.equal(root.get("userSession").get("id"), userSessionId);
    }

    public static Specification<ToolExecutionLog> hasLegacySessionId(String sessionId) {
        return textEquals("sessionId", sessionId);
    }

    private static Specification<ToolExecutionLog> textEquals(String attribute, String value) {
        if (!hasText(value)) return unrestricted();
        return (root, query, builder) ->
                builder.equal(builder.lower(root.get(attribute)), normalize(value));
    }

    private static Specification<ToolExecutionLog> unrestricted() {
        return (root, query, builder) -> builder.conjunction();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
