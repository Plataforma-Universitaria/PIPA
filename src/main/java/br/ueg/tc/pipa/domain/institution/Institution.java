package br.ueg.tc.pipa.domain.institution;

import br.ueg.tc.pipa.domain.preference.Preference;
import br.ueg.tc.pipa.generic.GenericModel;
import jakarta.persistence.*;
import lombok.*;

import javax.validation.constraints.Size;
import java.util.List;

import static jakarta.persistence.GenerationType.SEQUENCE;

@Entity
@Table(name = "institution")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Institution extends GenericModel implements IInstitution {

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

    @Column(name = "long_name", length = 100, nullable = false, unique = true)
    private String longName;

    @OneToOne(mappedBy = "institution", cascade = CascadeType.ALL, orphanRemoval = true)
    private Preference preference;

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
