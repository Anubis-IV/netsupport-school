package anubis.netsupport_school.backend.domain.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;


@Data
class ResultAnswerId implements Serializable {
    private Long result;   // Name must match the field name in ResultAnswer
    private Long question; // Name must match the field name in ResultAnswer
}

@Data
@Entity
@Table(name = "result_answers")
@IdClass(ResultAnswerId.class)
public class ResultAnswer {



    @Id
    @ManyToOne
    @JoinColumn(name = "result_id")
    private anubis.netsupport_school.backend.domain.model.Result result;

    @Id
    @ManyToOne
    @JoinColumn(name = "question_id")
    private anubis.netsupport_school.backend.domain.model.Question question;



    @Column(
            name = "selected_choice_id",
            nullable = false,
            columnDefinition = "CHECK (selected_choice_id >= 0 AND selected_choice_id <= 3)"
    )
    private Integer selectedChoiceId;
}
