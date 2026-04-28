package anubis.netsupport_school.backend.api.websocket;

import anubis.netsupport_school.backend.domain.dto.websocket.BaseMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.ClientDisconnectedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

@Slf4j
public class WebsocketConnectionHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper;
    private final MessageRouter router;

    public WebsocketConnectionHandler(ObjectMapper mapper, MessageRouter router) {
        this.mapper = mapper;
        this.router = router;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);

        BaseMessage baseMessage = mapper.readValue(message.getPayload(), BaseMessage.class);

        try {
            router.dispatch(session, baseMessage);
        }catch (Exception exception){
            log.error(exception.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        BaseMessage msg = new ClientDisconnectedMessage();
        try {
            router.dispatch(session, msg);
        }catch (Exception exception){
            log.error(exception.getMessage());
        }
    }
}
