package br.ueg.tc.pipa.domain.logs.Institution;

import br.ueg.tc.pipa.domain.enums.InstitutionEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "log_institution")
public class InstitutionContext {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(value = EnumType.STRING)
    private InstitutionEvent event;

    @Column(name = "institution_id")
    private Long institutionId;

    @Column(name = "details")
    private String details;

}
