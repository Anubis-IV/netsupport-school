package anubis.netsupport_school.backend.domain.dto.response;

import java.util.List;

public record ExamDetailsDTO(

        Long examId,
        String title,
        Integer durationMinutes,
        List<QuestionResponseDTO> questions

) {}
