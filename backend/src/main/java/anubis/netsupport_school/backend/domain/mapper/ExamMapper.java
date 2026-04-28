package anubis.netsupport_school.backend.domain.mapper;

import anubis.netsupport_school.backend.domain.dto.request.ExamRequestDTO;
import anubis.netsupport_school.backend.domain.dto.response.ExamDetailsDTO;
import anubis.netsupport_school.backend.domain.dto.response.ExamSummaryDTO;
import anubis.netsupport_school.backend.domain.dto.response.QuestionResponseDTO;
import anubis.netsupport_school.backend.domain.model.Exam;
import anubis.netsupport_school.backend.domain.model.Question;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExamMapper {

    public static Exam toEntity(ExamRequestDTO dto) {
        if (dto == null) return null;

        Exam exam = new Exam();
        exam.setTitle(dto.title());
        exam.setDurationMin(dto.durationMinutes());
        exam.setCreatedAt(LocalDateTime.now());

        // ننشئ لستة الأسئلة ونربطها بالـ exam
        if (dto.questions() != null) {
            List<Question> questions = dto.questions().stream()
                    .map(q -> QuestionMapper.toEntity(q, exam))
                    .toList();
            exam.setQuestions(new ArrayList<>(questions));
        } else {
            exam.setQuestions(new ArrayList<>());
        }

        return exam;
    }

    public static ExamSummaryDTO toSummaryDTO(Exam exam) {
        if (exam == null) return null;

        return new ExamSummaryDTO(
                exam.getExamId(),
                exam.getTitle(),
                exam.getDurationMin(),
                (exam.getQuestions() != null) ? exam.getQuestions().size() : 0,
                exam.getCreatedAt()
        );
    }

    public static ExamDetailsDTO toDetailsDTO(Exam exam) {
        if (exam == null) return null;

        // حماية ضد الـ Null في لستة الأسئلة
        List<QuestionResponseDTO> questions = (exam.getQuestions() != null)
                ? exam.getQuestions().stream()
                  .map(QuestionMapper::toDTO)
                  .toList()
                : new ArrayList<>();

        return new ExamDetailsDTO(
                exam.getExamId(),
                exam.getTitle(),
                exam.getDurationMin(),
                questions
        );
    }
}