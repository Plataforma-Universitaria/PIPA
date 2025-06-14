package br.ueg.tc.pipa.domain.accessData;

import br.ueg.tc.pipa.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccessDataRepository extends JpaRepository<AccessData,Long> {
   Optional<AccessData> findByKey(String key);
   Optional<AccessData> findByUserAndKey(User user, String key);

}
