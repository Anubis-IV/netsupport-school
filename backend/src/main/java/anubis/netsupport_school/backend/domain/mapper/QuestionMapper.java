package anubis.netsupport_school.backend.domain.mapper;

import anubis.netsupport_school.backend.domain.dto.request.QuestionRequestDTO;
import anubis.netsupport_school.backend.domain.dto.response.ChoiceResponseDTO;
import anubis.netsupport_school.backend.domain.dto.response.QuestionResponseDTO;
import anubis.netsupport_school.backend.domain.model.Exam;
import anubis.netsupport_school.backend.domain.model.Question;

import java.util.List;

public class QuestionMapper {

    public static Question toEntity(QuestionRequestDTO dto, Exam exam) {

        Question q = new Question();
        q.setText(dto.text());
        q.setExam(exam);

        // map 4 choices
        q.setChoice0(dto.choices().get(0));
        q.setChoice1(dto.choices().get(1));
        q.setChoice2(dto.choices().get(2));
        q.setChoice3(dto.choices().get(3));

        q.setCorrectChoiceId(dto.correctChoiceId());

        return q;
    }

    public static QuestionResponseDTO toDTO(Question q) {

        List<ChoiceResponseDTO> choices = List.of(
                new ChoiceResponseDTO(0, q.getChoice0()),
                new ChoiceResponseDTO(1, q.getChoice1()),
                new ChoiceResponseDTO(2, q.getChoice2()),
                new ChoiceResponseDTO(3, q.getChoice3())
        );

        return new QuestionResponseDTO(
                q.getQuestionId(),
                q.getText(),
                choices,
                q.getCorrectChoiceId()
        );
    }
}
