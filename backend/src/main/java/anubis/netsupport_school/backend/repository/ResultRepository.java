package anubis.netsupport_school.backend.repository;

import anubis.netsupport_school.backend.domain.model.Result;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByExamExamId(Long examId);

    Optional<Result> findByExamExamIdAndStudentId(Long examId, String studentId);

}
