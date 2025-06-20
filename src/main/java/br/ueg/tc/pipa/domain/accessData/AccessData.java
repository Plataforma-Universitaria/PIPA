package br.ueg.tc.pipa.domain.accessData;

import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.infra.generics.GenericModel;
import br.ueg.tc.pipa_integrator.interfaces.platform.IAccessData;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import static jakarta.persistence.GenerationType.SEQUENCE;

@Entity
@Table(name = "access_data")
@Getter
@Setter
public class AccessData extends GenericModel implements IAccessData {

    @SequenceGenerator(
            name = "access_data_generator_sequence",
            sequenceName = "access_data_sequence",
            allocationSize = 1
    )

    @GeneratedValue(
            strategy = SEQUENCE,
            generator = "access_data_generator_sequence"
    )

    @Id
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",
            referencedColumnName = "id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_access_data"))
    private User user;

    @Column(name = "token_key", nullable = false, length = 100)
    private String key;

    @Column(name = "token_value", nullable = false, length = 300)
    private String value;

}
