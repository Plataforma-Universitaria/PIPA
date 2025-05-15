package br.ueg.tc.pipa.intentManagement.domain.definations.impl;

import br.ueg.tc.pipa.intentManagement.domain.definations.IInstitution;
import br.ueg.tc.pipa.intentManagement.domain.definations.enums.Persona;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.util.List;

import static jakarta.persistence.GenerationType.SEQUENCE;

@Entity
@Table(name = "institution")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Institution implements IInstitution {

    @SequenceGenerator(
            name = "institution_generator_sequence",
            sequenceName = "institution_sequence",
            allocationSize = 1
    )

    @GeneratedValue(
            strategy = SEQUENCE,
            generator = "institution_generator_sequence"
    )

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "short_name", length = 10, nullable = false, unique = true)
    private String shortName;

    @Column(name = "salutation_phrase", length = 50, nullable = false)
    private String salutationPhrase;

    @Column(name = "provider_class", length = 50, nullable = false, unique = true)
    private String providerClass;

    @Column(name = "username_field_name", length = 15, nullable = false)
    private String usernameFieldName;

    @Column(name = "password_field_name", length = 15, nullable = false)
    private String passwordFieldName;

    @ElementCollection(targetClass = Persona.class, fetch = FetchType.EAGER)
    @CollectionTable(
            name = "institution_persona",
            joinColumns = @JoinColumn(name = "institution_id")
    )
    @Column(name = "persona", nullable = false)
    @Enumerated(EnumType.STRING)
    @Size(min = 1)
    private List<Persona> personas;

}
