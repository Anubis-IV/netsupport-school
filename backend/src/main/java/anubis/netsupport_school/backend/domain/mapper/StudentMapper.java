package anubis.netsupport_school.backend.domain.mapper;

import anubis.netsupport_school.backend.domain.dto.response.StudentDTO;
import anubis.netsupport_school.backend.domain.dto.response.StudentsResponseDTO;

import java.util.List;

public class StudentMapper {

    public static StudentDTO toDTO(
            String studentId,
            String studentName,
            String hostname,
            String status,
            boolean locked
    ) {
        return new StudentDTO(
                studentId,
                studentName,
                hostname,
                status,
                locked
        );
    }

    public static StudentsResponseDTO toResponse(List<StudentDTO> students) {
        return new StudentsResponseDTO(students);
    }
}
