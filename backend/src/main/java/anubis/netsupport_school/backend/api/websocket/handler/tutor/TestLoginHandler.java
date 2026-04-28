package anubis.netsupport_school.backend.api.websocket.handler.tutor;

import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.TestLoginMessage;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class TestLoginHandler implements MessageHandler<TestLoginMessage> {
    private final SessionService sessionService;

    public TestLoginHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void handle(WebSocketSession session, TestLoginMessage message) throws IOException {
        sessionService.broadcastToAllStudents(message);
    }

    @Override
    public Class<TestLoginMessage> getMessageType() {
        return TestLoginMessage.class;
    }
}
