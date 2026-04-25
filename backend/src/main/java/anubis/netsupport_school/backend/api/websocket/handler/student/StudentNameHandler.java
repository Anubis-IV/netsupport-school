package anubis.netsupport_school.backend.api.websocket.handler.student;

import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.StudentNameMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.StudentOnlineMessage;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class StudentNameHandler implements MessageHandler<StudentNameMessage> {
    private final SessionService sessionService;

    public StudentNameHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void handle(WebSocketSession session, StudentNameMessage message) throws IOException {
        session.getAttributes().put("name", message.name);

        var onlineMessage = new StudentOnlineMessage();
        onlineMessage.studentId = session.getId();
        onlineMessage.studentName = message.name;

        sessionService.broadcastToTutors(onlineMessage);
    }

    @Override
    public Class<StudentNameMessage> getMessageType() {
        return StudentNameMessage.class;
    }
}