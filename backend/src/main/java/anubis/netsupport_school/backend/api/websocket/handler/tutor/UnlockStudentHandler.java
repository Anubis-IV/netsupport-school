package anubis.netsupport_school.backend.api.websocket.handler.tutor;

import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.LockMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.LockStudentMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.UnlockMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.UnlockStudentMessage;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class UnlockStudentHandler implements MessageHandler<UnlockStudentMessage> {
    private final SessionService sessionService;

    public UnlockStudentHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void handle(WebSocketSession session, UnlockStudentMessage message) throws IOException {
        sessionService.broadcastToStudents(
                List.of(message.studentId),
                new UnlockMessage()
        );
    }

    @Override
    public Class<UnlockStudentMessage> getMessageType() {
        return UnlockStudentMessage.class;
    }
}
