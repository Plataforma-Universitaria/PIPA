package br.ueg.tc.pipa.domain.accessData;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessDataRepository extends JpaRepository<AccessData,Long> {
}
