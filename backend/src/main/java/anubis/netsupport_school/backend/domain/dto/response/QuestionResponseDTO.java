package anubis.netsupport_school.backend.domain.dto.response;

import java.util.List;

public record QuestionResponseDTO(

        Long questionId,
        String text,
        List<ChoiceResponseDTO> choices,
        Integer correctChoiceId

) {}
