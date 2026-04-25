package anubis.netsupport_school.backend.domain.dto.websocket;

import java.util.List;

public class SubmitAnswersMessage extends BaseMessage {
    public Long examId;
    public TriggerType trigger;  // enum: ANSWER_CHANGE, TIME_ENDED, TUTOR_STOPPED
    public List<AnswerDto> answers;
}
