package anubis.netsupport_school.backend.api.websocket.handler.tutor;

import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.LockMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.LockStudentMessage;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.List;

@Component
public class LockStudentHandler implements MessageHandler<LockStudentMessage> {
    private final SessionService sessionService;

    public LockStudentHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void handle(WebSocketSession session, LockStudentMessage message) throws IOException {
        sessionService.broadcastToStudents(
                List.of(message.studentId),
                new LockMessage()
        );
    }

    @Override
    public Class<LockStudentMessage> getMessageType() {
        return LockStudentMessage.class;
    }
}