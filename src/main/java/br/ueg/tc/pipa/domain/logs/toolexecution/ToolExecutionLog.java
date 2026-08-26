package br.ueg.tc.pipa.domain.logs.toolexecution;

import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.domain.usersession.UserSession;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureCategory;
import br.ueg.tc.pipa_integrator.observability.ProviderFailureStage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "tool_execution_log",
        indexes = {
                @Index(name = "idx_tool_execution_log_session", columnList = "user_session_id"),
                @Index(name = "idx_tool_execution_log_timestamp", columnList = "timestamp")
        }
)
public class ToolExecutionLog {

    @Id
    @SequenceGenerator(
            name = "tool_exec_log_seq",
            sequenceName = "tool_exec_log_sequence",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tool_exec_log_seq")
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_log_user"))
    private User user;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * Associação persistida da execução com a sessão histórica. É opcional
     * durante a transição para preservar logs criados antes desta coluna.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_session_id",
            foreignKey = @ForeignKey(name = "fk_tool_execution_log_user_session")
    )
    private UserSession userSession;

    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    @Column(name = "tool_version", length = 20)
    private String toolVersion;

    @Column(name = "persona", length = 50)
    private String persona;

    @Column(name = "result", length = 20)
    private String result;

    /** Tempo observado da execução. Logs históricos podem não possuir a medição. */
    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category", length = 50)
    private ProviderFailureCategory failureCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_stage", length = 50)
    private ProviderFailureStage failureStage;

    @Column(name = "retryable")
    private Boolean retryable;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
