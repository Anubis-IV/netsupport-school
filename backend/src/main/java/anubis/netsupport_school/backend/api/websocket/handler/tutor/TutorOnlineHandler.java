package anubis.netsupport_school.backend.api.websocket.handler.tutor;

import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.TutorOnlineMessage;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class TutorOnlineHandler implements MessageHandler<TutorOnlineMessage> {
    private final SessionService sessionService;

    public TutorOnlineHandler(SessionService service){
        this.sessionService = service;
    }

    @Override
    public void handle(WebSocketSession session, TutorOnlineMessage message) {
        sessionService.registerTutor(session);
    }

    @Override
    public Class<TutorOnlineMessage> getMessageType() {
        return TutorOnlineMessage.class;
    }
}
