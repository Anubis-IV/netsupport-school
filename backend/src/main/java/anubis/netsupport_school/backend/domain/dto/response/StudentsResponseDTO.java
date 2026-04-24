package anubis.netsupport_school.backend.domain.dto.response;

import java.util.List;

public record StudentsResponseDTO(
        List<StudentDTO> students
) {}
