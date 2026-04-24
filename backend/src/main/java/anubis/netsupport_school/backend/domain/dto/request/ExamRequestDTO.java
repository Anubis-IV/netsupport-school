package anubis.netsupport_school.backend.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ExamRequestDTO(

        @NotBlank(message = "Title is required")
        String title,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        Integer durationMinutes,

        @NotEmpty(message = "Exam must contain at least one question")
        @Valid
        List<QuestionRequestDTO> questions

) {}
