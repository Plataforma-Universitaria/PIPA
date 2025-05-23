package br.ueg.tc.pipa.domain.institution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface InstitutionRepository extends JpaRepository<Institution, Long> {
    Optional<Institution> findByShortNameIgnoreCase(String institutionName);
}
