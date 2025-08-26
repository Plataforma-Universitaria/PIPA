package br.ueg.tc.pipa.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
    User getByExternalKey(UUID externalKey);

    Optional<User> findByExternalKey(UUID uuid);
     Optional<User> deleteByExternalKey(UUID uuid);
}
