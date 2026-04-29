package anubis.netsupport_school.backend.api.websocket.handler.tutor;

import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.StudentOnlineMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.TutorOnlineMessage;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class TutorOnlineHandler implements MessageHandler<TutorOnlineMessage> {
    private final SessionService sessionService;

    public TutorOnlineHandler(SessionService service){
        this.sessionService = service;
    }

    @Override
    public void handle(WebSocketSession session, TutorOnlineMessage message) throws IOException {
        sessionService.registerTutor(session);

        for(var connectedStudent : sessionService.getConnectedStudents()){
            var onlineMessage = new StudentOnlineMessage();
            onlineMessage.studentId = session.getId();
            onlineMessage.studentName = (String)connectedStudent.getAttributes().get("name");

            sessionService.broadcastToTutor(session.getId(), onlineMessage);
        }
    }

    @Override
    public Class<TutorOnlineMessage> getMessageType() {
        return TutorOnlineMessage.class;
    }
}
