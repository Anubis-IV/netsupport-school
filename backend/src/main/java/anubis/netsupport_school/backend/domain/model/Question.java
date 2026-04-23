package anubis.netsupport_school.backend.domain.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @ManyToOne
    @JoinColumn(name = "exam_id")
    private anubis.netsupport_school.backend.domain.model.Exam exam;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "choice_0", nullable = false)
    private String choice0;

    @Column(name = "choice_1", nullable = false)
    private String choice1;

    @Column(name = "choice_2", nullable = false)
    private String choice2;

    @Column(name = "choice_3", nullable = false)
    private String choice3;

    @Column(
            name = "correct_choice_id",
            nullable = false,
            columnDefinition = "CHECK (correct_choice_id >= 0 AND correct_choice_id <= 3)"
    )
    private Integer correctChoiceId;



}