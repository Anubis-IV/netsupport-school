package anubis.netsupport_school.backend.service;

import anubis.netsupport_school.backend.domain.dto.websocket.BaseMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.StudentOnlineMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;

public interface SessionService {
    void registerTutor(WebSocketSession session);

    void broadcastToAllStudents(BaseMessage lockMessage) throws IOException;

    void broadcastToStudents(List<String> studentIds, BaseMessage studentMessage) throws IOException;

    void registerStudent(WebSocketSession session);

    int countConnectedStudents();

    void broadcastToTutors(BaseMessage message) throws IOException;

    WebSocketSession getStudent(String id);

    /** Returns a snapshot of all currently open student sessions. */
    List<WebSocketSession> getConnectedStudents();

    void broadcastToTutor(String id, BaseMessage onlineMessage) throws IOException;
}
