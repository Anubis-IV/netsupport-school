package anubis.netsupport_school.backend.api.rest;

import anubis.netsupport_school.backend.domain.dto.response.StudentDTO;
import anubis.netsupport_school.backend.domain.dto.response.StudentsResponseDTO;
import anubis.netsupport_school.backend.domain.mapper.StudentMapper;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final SessionService sessionService;

    public StudentController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    // =========================
    // GET ALL CONNECTED STUDENTS
    // =========================
    @GetMapping("/connected")
    public StudentsResponseDTO getConnectedStudents() {

        List<StudentDTO> students = sessionService.getConnectedStudents()
                .stream()
                .map(session -> {
                    String id       = session.getId();
                    String name     = (String) session.getAttributes().getOrDefault("name", "Unknown");
                    String hostname = (String) session.getAttributes().getOrDefault("hostname", "");
                    String status   = session.isOpen() ? "ONLINE" : "OFFLINE";
                    boolean locked  = Boolean.TRUE.equals(session.getAttributes().get("locked"));

                    return StudentMapper.toDTO(id, name, hostname, status, locked);
                })
                .toList();

        return StudentMapper.toResponse(students);
    }
}