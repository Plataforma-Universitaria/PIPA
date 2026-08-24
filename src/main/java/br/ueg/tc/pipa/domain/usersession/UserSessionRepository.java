package br.ueg.tc.pipa.domain.usersession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByFingerprint(String fingerprint);
    List<UserSession> findByUserId(Long userId);
}
