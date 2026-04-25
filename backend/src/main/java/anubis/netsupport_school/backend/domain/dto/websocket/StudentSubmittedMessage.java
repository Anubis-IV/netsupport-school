package anubis.netsupport_school.backend.domain.dto.websocket;

public class StudentSubmittedMessage extends BaseMessage {
    public String studentId;
    public String studentName;
    public Long examId;
    public TriggerType trigger;
    public int score;
    public int totalQuestions;
    public int answeredQuestions;
}
