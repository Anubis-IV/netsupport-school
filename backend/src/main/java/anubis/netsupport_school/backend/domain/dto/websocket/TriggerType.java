package anubis.netsupport_school.backend.domain.dto.websocket;

public enum TriggerType {
    ANSWER_CHANGE,   // student selected/changed an answer
    TIME_ENDED,      // countdown timer hit zero
    TUTOR_STOPPED    // server sent STOP_EXAM
}
