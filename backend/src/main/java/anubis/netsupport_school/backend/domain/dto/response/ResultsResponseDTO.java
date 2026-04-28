package anubis.netsupport_school.backend.domain.dto.response;

import java.util.List;

public record ResultsResponseDTO(

        Long examId,
        String examTitle,
        List<ResultItemDTO> results

) {}