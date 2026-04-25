package anubis.netsupport_school.backend.domain.dto.websocket;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LockMessage.class,          name = "LOCK"),
        @JsonSubTypes.Type(value = UnlockMessage.class,        name = "UNLOCK"),
        @JsonSubTypes.Type(value = StartExamMessage.class,     name = "START_EXAM"),
        @JsonSubTypes.Type(value = StopExamMessage.class,      name = "STOP_EXAM"),
        @JsonSubTypes.Type(value = RegisterMessage.class,      name = "REGISTER"),
        @JsonSubTypes.Type(value = SubmitAnswersMessage.class,  name = "SUBMIT_ANSWERS"),
        @JsonSubTypes.Type(value = StudentOnlineMessage.class,  name = "STUDENT_ONLINE"),
        @JsonSubTypes.Type(value = StudentOfflineMessage.class, name = "STUDENT_OFFLINE"),
        @JsonSubTypes.Type(value = StudentSubmittedMessage.class, name = "STUDENT_SUBMITTED"),
        @JsonSubTypes.Type(value = TestLoginMessage.class,   name = "TEST_LOGIN"),
        @JsonSubTypes.Type(value = StudentNameMessage.class,   name = "STUDENT_NAME"),
        @JsonSubTypes.Type(value = LockAllMessage.class,       name = "LOCK_ALL"),
        @JsonSubTypes.Type(value = UnlockAllMessage.class,     name = "UNLOCK_ALL"),
        @JsonSubTypes.Type(value = LockStudentMessage.class,   name = "LOCK_STUDENT"),
        @JsonSubTypes.Type(value = UnlockStudentMessage.class, name = "UNLOCK_STUDENT"),
        @JsonSubTypes.Type(value = ScanStudentsMessage.class, name = "SCAN_STUDENTS"),
        @JsonSubTypes.Type(value = TutorOnlineMessage.class, name = "TUTOR_ONLINE")
})
public abstract class BaseMessage {}

