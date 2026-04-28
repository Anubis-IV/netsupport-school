package anubis.netsupportschool.studentapp;

import java.util.List;

/**
 * Parsed representation of the START_EXAM message payload.
 */
public class ExamData {

    public final long          examId;
    public final String        title;
    public final int           durationMinutes;
    public final boolean       requireNameEntry;
    public final List<Question> questions;

    public ExamData(long examId, String title, int durationMinutes,
                    boolean requireNameEntry, List<Question> questions) {
        this.examId          = examId;
        this.title           = title;
        this.durationMinutes = durationMinutes;
        this.requireNameEntry = requireNameEntry;
        this.questions       = questions;
    }

    // ── Nested DTO ───────────────────────────────────────────────────────────

    public static class Question {
        public final long         questionId;
        public final String       text;
        public final List<Choice> choices;

        public Question(long questionId, String text, List<Choice> choices) {
            this.questionId = questionId;
            this.text       = text;
            this.choices    = choices;
        }
    }

    public static class Choice {
        public final int    choiceId;
        public final String text;

        public Choice(int choiceId, String text) {
            this.choiceId = choiceId;
            this.text     = text;
        }
    }
}
