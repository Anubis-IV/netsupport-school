package anubis.netsupport_school.backend.service;

import anubis.netsupport_school.backend.domain.dto.websocket.BaseMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.LockMessage;
import anubis.netsupport_school.backend.repository.SessionRepository;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Service @Slf4j
public class SessionServiceImpl implements SessionService {
    private final int sendTimeout = 1000;
    private final int bufferSizeLimit = 1024 * 1024;
    private final SessionRepository sessionRepository;
    private final ObjectMapper mapper;

    public SessionServiceImpl(SessionRepository sessionRepository, ObjectMapper mapper) {
        this.sessionRepository = sessionRepository;
        this.mapper = mapper;
    }

    @Override
    public void registerTutor(WebSocketSession session) {
        sessionRepository.addTutorSession(new ConcurrentWebSocketSessionDecorator(session, sendTimeout, bufferSizeLimit) );
    }

    @Override
    public void broadcastToAllStudents(BaseMessage message) throws IOException {
        updateRepository();

        String json = mapper.writeValueAsString(message);

        for (WebSocketSession session : sessionRepository.getStudents()){
            session.sendMessage(new TextMessage(json));
            log.debug("Sent: " + json + ", to student: " + session.getId());
        }
    }

    @Override
    public void broadcastToStudents(List<String> studentIds, BaseMessage studentMessage) throws IOException {
        updateRepository();
        String json = mapper.writeValueAsString(studentMessage);

        for (String id : studentIds){
            var session = sessionRepository.getStudent(id);
            session.sendMessage(new TextMessage(json));
        }
    }

    @Override
    public void registerStudent(WebSocketSession session) {
        sessionRepository.addStudentSession(new ConcurrentWebSocketSessionDecorator(session, sendTimeout, bufferSizeLimit));
    }

    @Override
    public int countConnectedStudents() {
        updateRepository();
        return sessionRepository.getStudents().size();
    }

    @Override
    public void broadcastToTutors(BaseMessage message) throws IOException {
        updateRepository();
        String json = mapper.writeValueAsString(message);

        for (WebSocketSession session : sessionRepository.getTutors()){
            session.sendMessage(new TextMessage(json));
        }
    }

    @Override
    public WebSocketSession getStudent(String id) {
        return sessionRepository.getStudent(id);
    }

    private void updateRepository(){
        for(var session : sessionRepository.getStudents()){
            if(!session.isOpen())
                sessionRepository.removeStudentSession(session);
        }
        for(var session : sessionRepository.getTutors()){
            if(!session.isOpen())
                sessionRepository.removeTutorSession(session);
        }
    }

    @Override
    public List<WebSocketSession> getConnectedStudents() {
        updateRepository();                        // prune closed sessions first
        return sessionRepository.getStudents();    // already a defensive copy
    }

    @Override
    public void broadcastToTutor(String id, BaseMessage onlineMessage) throws IOException {
        updateRepository();
        var session = sessionRepository.getTutor(id);

        if(session.isOpen()) {
            String json = mapper.writeValueAsString(onlineMessage);
            session.sendMessage(new TextMessage(json));
        }
    }


}
