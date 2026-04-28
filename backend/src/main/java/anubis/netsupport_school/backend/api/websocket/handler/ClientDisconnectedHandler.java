package anubis.netsupport_school.backend.api.websocket.handler;

import anubis.netsupport_school.backend.domain.dto.websocket.ClientDisconnectedMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.StudentOfflineMessage;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.io.IOException;

public class ClientDisconnectedHandler implements MessageHandler<ClientDisconnectedMessage> {
    private final SessionService sessionService;

    public ClientDisconnectedHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void handle(WebSocketSession session, ClientDisconnectedMessage message) throws IOException {
        String type = (String) session.getAttributes().getOrDefault("type", "");

        if("student".equals(type)){
            StudentOfflineMessage msg = new StudentOfflineMessage();
            msg.studentId = session.getId();

            sessionService.broadcastToTutors(msg);
        }


    }

    @Override
    public Class<ClientDisconnectedMessage> getMessageType() {
        return ClientDisconnectedMessage.class;
    }
}
