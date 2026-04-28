package anubis.netsupport_school.backend.domain.dto.response;

import java.util.List;

public record ExamsResponseDTO(
        List<ExamSummaryDTO> exams
) {}
