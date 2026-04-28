package anubis.netsupport_school.backend.domain.mapper;

import anubis.netsupport_school.backend.domain.dto.response.ResultItemDTO;
import anubis.netsupport_school.backend.domain.dto.response.ResultsResponseDTO;
import anubis.netsupport_school.backend.domain.model.Result;

import java.util.List;

public class ResultMapper {

    public static ResultItemDTO toDTO(Result r) {
        return new ResultItemDTO(
                r.getStudentId(),
                r.getStudentName(),
                r.getHostname(),
                r.getScore(),
                r.getTotalQuestions(),
                r.getAnsweredQuestions(),
                r.getSubmittedAt()
        );
    }

    public static ResultsResponseDTO toResponse(Long examId, String examTitle, List<Result> results) {

        List<ResultItemDTO> items = results.stream()
                .map(ResultMapper::toDTO)
                .toList();

        return new ResultsResponseDTO(
                examId,
                examTitle,
                items
        );
    }
}