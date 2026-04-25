package anubis.netsupport_school.backend.api.websocket;

import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.BaseMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MessageRouter {
    private final Map<Class<?>, MessageHandler<?>> handlers = new HashMap<>();

    public MessageRouter(List<MessageHandler<?>> handlerList) {
        for (var h : handlerList) {
            handlers.put(h.getMessageType(), h);
        }
    }

    @SuppressWarnings("unchecked")
    public void dispatch(WebSocketSession session, BaseMessage msg) throws IOException {
        MessageHandler<BaseMessage> handler = (MessageHandler<BaseMessage>) handlers.get(msg.getClass());

        if (handler == null) {
            throw new RuntimeException("No handler for " + msg.getClass());
        }

        handler.handle(session, msg);
    }
}
