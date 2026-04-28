package anubis.netsupport_school.backend.domain.dto.response;

public record StudentDTO(

        String studentId,
        String studentName,
        String hostname,
        String status,
        boolean locked

) {}
