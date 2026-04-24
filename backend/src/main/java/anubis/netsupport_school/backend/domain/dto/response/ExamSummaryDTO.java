package anubis.netsupport_school.backend.domain.dto.response;

import java.time.LocalDateTime;

public record ExamSummaryDTO(
        Long examId,
        String title,
        Integer durationMinutes,
        Integer questionCount,
        LocalDateTime createdAt
) {}
