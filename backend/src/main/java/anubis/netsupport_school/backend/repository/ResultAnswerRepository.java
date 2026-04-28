package anubis.netsupport_school.backend.repository;

import anubis.netsupport_school.backend.domain.model.ResultAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultAnswerRepository extends JpaRepository<ResultAnswer, Long> {

    List<ResultAnswer> findByResultResultId(Long resultId);

    void deleteByResultResultId(Long resultId);

}
