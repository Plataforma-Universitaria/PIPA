package br.ueg.tc.pipa.domain.preference;

import br.ueg.tc.pipa.domain.institution.Institution;
import br.ueg.tc.pipa_integrator.institutions.definations.IPreference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "preference")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Preference implements IPreference {

    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private Institution institution;

    @Column(name = "salutation_phrase", length = 50, nullable = false)
    private String salutationPhrase;

    @Column(name = "username_field_name", length = 15, nullable = false)
    private String usernameFieldName;

    @Column(name = "password_field_name", length = 15, nullable = false)
    private String passwordFieldName;

    @Column(name = "provider_class", length = 50, nullable = false, unique = true)
    private String providerClass;

    @Column(name = "provider_path", length = 50, nullable = false, unique = true)
    private String providerPath;


}
