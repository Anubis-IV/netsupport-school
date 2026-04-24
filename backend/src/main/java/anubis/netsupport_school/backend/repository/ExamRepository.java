package anubis.netsupport_school.backend.repository;

import anubis.netsupport_school.backend.domain.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Long> {
}
