package br.ueg.tc.pipa.domain.institution;

import br.ueg.tc.pipa.domain.preference.Preference;
import br.ueg.tc.pipa.generic.GenericModel;
import br.ueg.tc.pipa_integrator.enums.Persona;
import br.ueg.tc.pipa_integrator.institutions.definations.IInstitution;
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

    @Column(name = "provider_class", length = 50, nullable = false, unique = true)
    private String providerClass;

    @Column(name = "provider_path", length = 50, nullable = false, unique = true)
    private String providerPath;

}
