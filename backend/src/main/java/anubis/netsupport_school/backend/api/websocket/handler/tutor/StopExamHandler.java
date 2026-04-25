package anubis.netsupport_school.backend.api.websocket.handler.tutor;

import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.StopExamMessage;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class StopExamHandler implements MessageHandler<StopExamMessage> {
    private final SessionService sessionService;

    public StopExamHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void handle(WebSocketSession session, StopExamMessage message) throws IOException {
        sessionService.broadcastToAllStudents(message);
    }

    @Override
    public Class<StopExamMessage> getMessageType() {
        return StopExamMessage.class;
    }
}
