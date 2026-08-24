package br.ueg.tc.pipa.domain.logs.toolexecution;

import br.ueg.tc.pipa.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tool_execution_log")
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

    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    @Column(name = "tool_version", length = 20)
    private String toolVersion;

    @Column(name = "persona", length = 50)
    private String persona;

    @Column(name = "result", length = 20)
    private String result;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
