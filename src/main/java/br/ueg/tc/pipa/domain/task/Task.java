package br.ueg.tc.pipa.domain.task;

import br.ueg.tc.pipa.domain.user.User;
import br.ueg.tc.pipa.infra.generics.GenericModel;
import br.ueg.tc.pipa.infra.utils.DateFormatter;
import br.ueg.tc.pipa_integrator.interfaces.platform.ITask;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.SEQUENCE;

@Entity
@Getter
@Setter
@Table(name = "task")
@NoArgsConstructor
@AllArgsConstructor
public class Task extends GenericModel implements ITask {

    @SequenceGenerator(
            name = "task_generator_sequence",
            sequenceName = "task_sequence",
            allocationSize = 1
    )

    @GeneratedValue(
            strategy = SEQUENCE,
            generator = "task_generator_sequence"
    )

    @Id
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_task_user"))
    private User user;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Column(name = "note", nullable = false)
    private String note;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("*Anotação do dia* ")
                .append("_" + DateFormatter.format(getDate()) + "_")
                .append(":\n")
                .append(getNote());
        return builder.toString();
    }
}
