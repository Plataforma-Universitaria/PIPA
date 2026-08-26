package br.ueg.tc.pipa.domain.usersession;

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
@Table(
        name = "user_session",
        indexes = {
                @Index(name = "idx_user_session_fingerprint", columnList = "fingerprint"),
                @Index(
                        name = "idx_user_session_active_lookup",
                        columnList = "user_id,fingerprint,channel,ended_at,last_activity_at"
                )
        }
)
public class UserSession {

    @Id
    @SequenceGenerator(
            name = "user_session_seq",
            sequenceName = "user_session_sequence",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_session_seq")
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_session_user"))
    private User user;

    @Column(name = "channel", length = 50)
    private String channel;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /**
     * Nullable no schema para manter compatibilidade com registros anteriores.
     * Novas entidades recebem o valor automaticamente em {@link #initializeActivityTimestamps()}.
     */
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "fingerprint", length = 100)
    private String fingerprint;

    @PrePersist
    void initializeActivityTimestamps() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (lastActivityAt == null) {
            lastActivityAt = startedAt;
        }
    }
}
