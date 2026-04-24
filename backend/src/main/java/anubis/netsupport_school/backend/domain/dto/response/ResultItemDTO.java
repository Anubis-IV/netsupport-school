package anubis.netsupport_school.backend.domain.dto.response;

import java.time.LocalDateTime;

public record ResultItemDTO(

        String studentId,
        String studentName,
        String hostname,
        Integer score,
        Integer totalQuestions,
        Integer answeredQuestions,
        LocalDateTime submittedAt

) {}
