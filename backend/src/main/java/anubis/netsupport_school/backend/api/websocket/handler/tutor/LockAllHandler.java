package anubis.netsupport_school.backend.api.websocket.handler.tutor;

import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.LockAllMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.LockMessage;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class LockAllHandler implements MessageHandler<LockAllMessage> {
    private final SessionService sessionService;

    public LockAllHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void handle(WebSocketSession session, LockAllMessage message) throws IOException {
        sessionService.broadcastToAllStudents(new LockMessage());
    }

    @Override
    public Class<LockAllMessage> getMessageType() {
        return LockAllMessage.class;
    }
}
