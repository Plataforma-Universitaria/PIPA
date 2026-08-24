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
@Table(name = "user_session")
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

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "fingerprint", length = 100)
    private String fingerprint;
}
