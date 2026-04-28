package anubis.netsupport_school.backend.domain.dto.request;

import jakarta.validation.constraints.*;

import java.util.List;

public record QuestionRequestDTO(

        @NotBlank(message = "Question text is required")
        String text,

        @NotNull(message = "Choices are required")
        @Size(min = 4, max = 4, message = "Exactly 4 choices are required")
        List<@NotBlank(message = "Choice cannot be empty") String> choices,

        @Min(value = 0, message = "correctChoiceId must be between 0 and 3")
        @Max(value = 3, message = "correctChoiceId must be between 0 and 3")
        Integer correctChoiceId

) {}
