package anubis.netsupport_school.backend.domain.dto.websocket;

import java.util.List;

public class TutorStartExamMessage extends BaseMessage {
    public Long examId;
    public List<String> studentIds;
    public boolean requireNameEntry;
}
