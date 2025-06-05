package br.ueg.tc.pipa.domain.user;

import br.ueg.tc.pipa.domain.accesdata.AccessData;
import br.ueg.tc.pipa.domain.institution.Institution;
import br.ueg.tc.pipa.infra.generics.GenericModel;
import br.ueg.tc.pipa_integrator.institutions.KeyValue;
import br.ueg.tc.pipa_integrator.institutions.definations.IAccessData;
import br.ueg.tc.pipa_integrator.institutions.definations.IInstitution;
import br.ueg.tc.pipa_integrator.institutions.definations.IUser;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.GenerationType.SEQUENCE;

@Entity
@Getter
@Setter
@Table(name = "user_")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends GenericModel implements IUser {

    @SequenceGenerator(
            name = "user_generator_sequence",
            sequenceName = "user_data_sequence",
            allocationSize = 1
    )

    @GeneratedValue(
            strategy = SEQUENCE,
            generator = "user_generator_sequence"
    )

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "external_id", length = 40, nullable = false, unique = true)
    private UUID externalKey;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_institution"))
    private Institution institution;

    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<AccessData> accessData;

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_personas",
            joinColumns = @JoinColumn(name = "user_id"),
            foreignKey = @ForeignKey(name = "fk_user_persona")
    )
    @Column(name = "persona", nullable = false)
    private List<String> personas;

    @Override
    public void setAccessData(Set<? extends IAccessData> accessData) {
        this.accessData = (Set<AccessData>) accessData;
    }

    @Transient
    private List<KeyValue> keyValueList = new ArrayList<>();

    public List<KeyValue> getKeyValueList() {

        if (this.keyValueList == null || this.keyValueList.isEmpty()) {
            List<KeyValue> keyValues = new ArrayList<>();

            for (AccessData accessData : this.accessData) {
                KeyValue keyValue = KeyValue.builder()
                        .key(accessData.getKey())
                        .value(accessData.getValue())
                        .build();
                keyValues.add(keyValue);
            }
            setKeyValueList(keyValues);
            return this.keyValueList;
        }

        return this.keyValueList;
    }

    public IInstitution getEducationalInstitution() {
        return institution;
    }
}
