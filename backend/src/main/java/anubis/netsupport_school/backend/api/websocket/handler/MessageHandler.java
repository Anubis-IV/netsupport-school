package anubis.netsupport_school.backend.api.websocket.handler;

import anubis.netsupport_school.backend.domain.dto.websocket.BaseMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

public interface MessageHandler<T extends BaseMessage> {
    void handle(WebSocketSession session, T message) throws IOException;
    Class<T> getMessageType();
}