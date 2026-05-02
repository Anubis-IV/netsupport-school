package anubis.netsupport_school.backend.service;

import anubis.netsupport_school.backend.domain.dto.websocket.*;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service @Slf4j
public class SessionServiceImpl implements SessionService {
    private final ConcurrentHashMap<Long, Set<String>> pendingSubmissions = new ConcurrentHashMap<>();
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



    @Override
    public void registerExamSession(Long examId, List<String> studentIds) {
        // called when tutor starts an exam
        pendingSubmissions.put(examId, ConcurrentHashMap.newKeySet());
        pendingSubmissions.get(examId).addAll(studentIds);
    }

    @Override
    public void handleStudentSubmitted(Long examId, String studentId) throws IOException {
        Set<String> pending = pendingSubmissions.get(examId);
        if (pending == null) return;

        // Remove this student from pending set
        pending.remove(studentId);

        // Reset student name to default on server side
        WebSocketSession session = sessionRepository.getStudent(studentId);
        if (session != null) {
            int studentNumber = getStudentNumber(studentId);
            session.getAttributes().put("name", "Student " + studentNumber);

            // Notify tutor of name reset
            StudentOnlineMessage resetMsg = new StudentOnlineMessage();
            resetMsg.studentId = studentId;
            resetMsg.studentName = "Student " + studentNumber;

            broadcastToTutors(resetMsg);
        }

        // If all students submitted → auto stop exam
        if (pending.isEmpty()) {
            pendingSubmissions.remove(examId);

            // Tell all students exam is done
            StopExamMessage stopMsg = new StopExamMessage();
            broadcastToAllStudents(stopMsg);

            // Tell tutor exam is completed
            ExamCompletedMessage completedMsg = new ExamCompletedMessage();
            completedMsg.examId = examId;
            broadcastToTutors(completedMsg);
        }
    }

    @Override
    public void disconnectAllStudents() throws IOException {
        updateRepository();
        for (WebSocketSession session : sessionRepository.getStudents()) {
            if (session.isOpen()) {
                // Send disconnect command first so student-app can clean up
                DisconnectAllMessage msg = new DisconnectAllMessage();
                String json = mapper.writeValueAsString(msg);
                session.sendMessage(new TextMessage(json));
                session.close();
            }
        }
    }

    private int getStudentNumber(String studentId) {
        return sessionRepository.getStudents()
                .stream()
                .map(WebSocketSession::getId)
                .toList()
                .indexOf(studentId) + 1;
    }

}
