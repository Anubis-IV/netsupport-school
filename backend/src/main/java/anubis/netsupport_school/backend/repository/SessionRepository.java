package anubis.netsupport_school.backend.repository;

import org.springframework.web.socket.WebSocketSession;

import java.util.HashSet;
import java.util.List;

public interface SessionRepository {
    boolean addStudentSession(WebSocketSession session);
    void removeStudentSession(WebSocketSession session);
    List<WebSocketSession> getStudents();
    boolean addTutorSession(WebSocketSession session);
    void removeTutorSession(WebSocketSession session);
    List<WebSocketSession> getTutors();

    WebSocketSession getStudent(String id);
}
