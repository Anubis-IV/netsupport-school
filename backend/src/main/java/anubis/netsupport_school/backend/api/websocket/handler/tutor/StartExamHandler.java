package anubis.netsupport_school.backend.api.websocket.handler.tutor;

import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.StartExamMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.TutorStartExamMessage;
import anubis.netsupport_school.backend.domain.mapper.QuestionMapper;
import anubis.netsupport_school.backend.domain.model.Exam;
import anubis.netsupport_school.backend.service.ExamService;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class StartExamHandler implements MessageHandler<TutorStartExamMessage> {
    private final SessionService sessionService;
    private final ExamService examService;

    public StartExamHandler(SessionService sessionService, ExamService examService) {
        this.sessionService = sessionService;
        this.examService = examService;
    }

    @Override
    public void handle(WebSocketSession session, TutorStartExamMessage message) throws IOException {
        Exam ex = examService.getExamOrThrow(message.examId);

        StartExamMessage studentMessage = new StartExamMessage();
        studentMessage.examId = ex.getExamId();
        studentMessage.title = ex.getTitle();
        studentMessage.durationMinutes = ex.getDurationMin();
        studentMessage.questions = ex.getQuestions().stream().map(QuestionMapper::toDTO).toList();
        studentMessage.requireNameEntry = message.requireNameEntry;

        sessionService.broadcastToStudents(message.studentIds, studentMessage);
    }

    @Override
    public Class<TutorStartExamMessage> getMessageType() {
        return TutorStartExamMessage.class;
    }
}
