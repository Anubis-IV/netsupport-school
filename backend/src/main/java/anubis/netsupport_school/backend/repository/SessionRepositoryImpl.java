package anubis.netsupport_school.backend.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SessionRepositoryImpl implements SessionRepository{
    private final ConcurrentHashMap<String, WebSocketSession> studentsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketSession> tutorsMap = new ConcurrentHashMap<>();

    @Override
    public boolean addStudentSession(WebSocketSession session) {
        return studentsMap.put(session.getId(), session) == null;
    }

    @Override
    public void removeStudentSession(WebSocketSession session) {
        studentsMap.remove(session.getId());
    }

    @Override
    public List<WebSocketSession> getStudents() {
        return List.copyOf(studentsMap.values());
    }

    @Override
    public boolean addTutorSession(WebSocketSession session) {
        return tutorsMap.put(session.getId(), session) == null;
    }

    @Override
    public void removeTutorSession(WebSocketSession session) {
        tutorsMap.remove(session.getId());
    }

    @Override
    public List<WebSocketSession> getTutors() {
        return List.copyOf(tutorsMap.values());
    }

    @Override
    public WebSocketSession getStudent(String id) {
        return studentsMap.get(id);
    }
}
