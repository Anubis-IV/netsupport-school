package anubis.netsupport_school.backend.repository;

import anubis.netsupport_school.backend.domain.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByExam_ExamId(Long examId);

}
