package anubis.netsupport_school.backend.domain.dto.websocket;

import anubis.netsupport_school.backend.domain.dto.response.QuestionResponseDTO;

import java.util.List;

public class StartExamMessage extends BaseMessage {
    public Long examId;
    public String title;
    public int durationMinutes;
    public boolean requireNameEntry;   // true → show name entry screen first
    public List<QuestionResponseDTO> questions;
}
