package anubis.netsupport_school.backend.api.websocket.handler.tutor;

import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.UnlockAllMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.UnlockMessage;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class UnlockAllHandler implements MessageHandler<UnlockAllMessage> {
    private final SessionService sessionService;

    public UnlockAllHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void handle(WebSocketSession session, UnlockAllMessage message) throws IOException {
        sessionService.broadcastToAllStudents(new UnlockMessage());
    }

    @Override
    public Class<UnlockAllMessage> getMessageType() {
        return UnlockAllMessage.class;
    }
}
