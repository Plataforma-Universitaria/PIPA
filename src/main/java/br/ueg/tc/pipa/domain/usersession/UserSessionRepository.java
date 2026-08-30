package br.ueg.tc.pipa.domain.usersession;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session from UserSession session
             where session.user.id = :userId
               and session.fingerprint = :fingerprint
               and session.channel = :channel
               and session.endedAt is null
             order by session.lastActivityAt desc
            """)
    List<UserSession> findActiveForUpdate(
            @Param("userId") Long userId,
            @Param("fingerprint") String fingerprint,
            @Param("channel") String channel
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session from UserSession session
             where session.endedAt is null
               and (
                    session.lastActivityAt <= :cutoff
                    or (session.lastActivityAt is null and session.startedAt <= :cutoff)
               )
            """)
    List<UserSession> findInactiveForUpdate(@Param("cutoff") LocalDateTime cutoff);

    List<UserSession> findByUserId(Long userId);
}
