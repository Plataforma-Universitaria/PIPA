package br.ueg.tc.pipa.domain.logs.Institution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InstitutionContextRepository extends JpaRepository<InstitutionContext, UUID> {
}
