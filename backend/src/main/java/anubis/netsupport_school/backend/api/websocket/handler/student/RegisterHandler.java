package anubis.netsupport_school.backend.api.websocket.handler.student;

import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.RegisterMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.StudentOnlineMessage;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class RegisterHandler implements MessageHandler<RegisterMessage> {
    private final SessionService sessionService;

    public RegisterHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void handle(WebSocketSession session, RegisterMessage message) throws IOException {
        int count = sessionService.countConnectedStudents();
        String name = "Student " + count;
        session.getAttributes().put("name", name);
        session.getAttributes().put("type", "student");

        sessionService.registerStudent(session);

        var onlineMessage = new StudentOnlineMessage();
        onlineMessage.studentId = session.getId();
        onlineMessage.studentName = name;

        sessionService.broadcastToTutors(onlineMessage);
    }

    @Override
    public Class<RegisterMessage> getMessageType() {
        return RegisterMessage.class;
    }
}
