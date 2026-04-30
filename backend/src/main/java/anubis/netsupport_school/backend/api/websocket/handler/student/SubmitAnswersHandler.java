package anubis.netsupport_school.backend.api.websocket.handler.student;

import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.StudentSubmittedMessage;
import anubis.netsupport_school.backend.domain.dto.websocket.SubmitAnswersMessage;
import anubis.netsupport_school.backend.service.ResultService;
import anubis.netsupport_school.backend.service.SessionService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class SubmitAnswersHandler implements MessageHandler<SubmitAnswersMessage> {
    private final ResultService resultService;
    private final SessionService sessionService;

    public SubmitAnswersHandler(ResultService resultService, SessionService sessionService) {
        this.resultService = resultService;
        this.sessionService = sessionService;
    }

    @Override
    public void handle(WebSocketSession session, SubmitAnswersMessage message) throws IOException {
        var result = resultService.saveOrUpdateAnswers(
                message.examId,
                session.getId(),
                (String) session.getAttributes().get("name"),
                "",
                message.answers.stream().collect(Collectors.toMap(answer -> answer.questionId,answer -> answer.selectedChoiceId))
        );

        var messageToTutor =  new StudentSubmittedMessage();
        messageToTutor.examId = message.examId;
        messageToTutor.answeredQuestions = result.getAnsweredQuestions();
        messageToTutor.studentId = result.getStudentId();
        messageToTutor.studentName = result.getStudentName();
        messageToTutor.trigger = message.trigger;
        messageToTutor.totalQuestions = result.getTotalQuestions();
        messageToTutor.score = result.getScore();

        sessionService.broadcastToTutors(messageToTutor);
    }

    @Override
    public Class<SubmitAnswersMessage> getMessageType() {
        return SubmitAnswersMessage.class;
    }
}
