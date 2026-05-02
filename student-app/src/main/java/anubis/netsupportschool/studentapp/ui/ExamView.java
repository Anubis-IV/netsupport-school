package anubis.netsupportschool.studentapp.ui;

import anubis.netsupportschool.studentapp.ExamData;
import anubis.netsupportschool.studentapp.StudentApplication;
import anubis.netsupportschool.studentapp.service.WebSocketService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Full exam UI:
 *   - Header bar  : exam title + countdown timer
 *   - Left panel  : question navigator (numbered buttons)
 *   - Centre panel: current question + 4 radio choices
 *   - Footer bar  : Previous / Next / Submit buttons
 *
 * Answers are submitted automatically when:
 *   a) The student clicks "Submit Exam"
 *   b) The timer reaches zero  (trigger = TIME_ENDED)
 *   c) The server sends STOP_EXAM → app.stopExam() → forceSubmit()  (trigger = TUTOR_STOPPED)
 *
 * On every answer selection answers are also sent as ANSWER_CHANGE so the
 * tutor can see live progress.
 *
 * FIXES:
 *   - ConcurrentModificationException: Detach RadioButtons before clearing group
 *   - Proper cleanup: Clear exam view and stop timer when exam ends
 */
public class ExamView {

    private final ExamData exam;
    private final WebSocketService wsService;

    // Answer state: questionId → selectedChoiceId  (-1 = unanswered)
    private final Map<Long, Integer> answers = new HashMap<>();

    private int currentIndex = 0;

    // UI nodes
    private final BorderPane root;
    private final Text timerText;
    private final VBox navPanel;
    private final ToggleGroup choiceGroup = new ToggleGroup();
    private final VBox questionArea;
    private final Button[] navButtons;
    private Timeline countdownTimeline = null;

    private final AtomicBoolean submitted = new AtomicBoolean(false);

    public boolean isExamActive() {
        return isExamActive.get();
    }

    private final AtomicBoolean isExamActive = new AtomicBoolean(true);
    private final StudentApplication studentApplication;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ExamView(ExamData exam, WebSocketService wsService, StudentApplication studentApplication) {
        this.exam = exam;
        this.wsService = wsService;
        this.studentApplication = studentApplication;

        // Initialise answers map
        for (ExamData.Question q : exam.questions) {
            answers.put(q.questionId, -1);
        }

        root = new BorderPane();
        root.getStyleClass().add("exam-root");

        // ── Header ────────────────────────────────────────────────────────────
        HBox header = new HBox();
        header.getStyleClass().add("exam-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 24, 0, 24));

        Text examTitle = new Text(exam.title);
        examTitle.getStyleClass().add("exam-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox timerBox = new VBox(2);
        timerBox.setAlignment(Pos.CENTER_RIGHT);
        Text timerLabel = new Text("TIME REMAINING");
        timerLabel.getStyleClass().add("timer-label");
        timerText = new Text(formatTime(exam.durationMinutes * 60));
        timerText.getStyleClass().add("timer-value");
        timerBox.getChildren().addAll(timerLabel, timerText);

        header.getChildren().addAll(examTitle, spacer, timerBox);
        root.setTop(header);

        // ── Navigator panel (left) ────────────────────────────────────────────
        navPanel = new VBox(8);
        navPanel.getStyleClass().add("nav-panel");
        navPanel.setPadding(new Insets(16));
        navButtons = new Button[exam.questions.size()];
        buildNavButtons();

        ScrollPane navScroll = new ScrollPane(navPanel);
        navScroll.setFitToWidth(true);
        navScroll.getStyleClass().add("nav-scroll");
        navScroll.setPrefWidth(120);
        root.setLeft(navScroll);

        // ── Question area (centre) ────────────────────────────────────────────
        questionArea = new VBox(20);
        questionArea.getStyleClass().add("question-area");
        questionArea.setPadding(new Insets(32, 48, 32, 48));

        ScrollPane qScroll = new ScrollPane(questionArea);
        qScroll.setFitToWidth(true);
        qScroll.getStyleClass().add("q-scroll");
        root.setCenter(qScroll);

        showQuestion(0);

        // ── Footer ────────────────────────────────────────────────────────────
        HBox footer = new HBox(12);
        footer.getStyleClass().add("exam-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 24, 12, 24));

        Button prevBtn = new Button("← Previous");
        Button nextBtn = new Button("Next →");
        Button submitBtn = new Button("Submit Exam");
        prevBtn.getStyleClass().add("btn-secondary");
        nextBtn.getStyleClass().add("btn-secondary");
        submitBtn.getStyleClass().add("btn-primary");

        prevBtn.setOnAction(e -> navigate(-1));
        nextBtn.setOnAction(e -> navigate(1));
        submitBtn.setOnAction(e -> confirmAndSubmit());

        // Progress label
        Text progressText = new Text();
        progressText.getStyleClass().add("progress-text");
        updateProgress(progressText);

        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);

        footer.getChildren().addAll(progressText, fSpacer, prevBtn, nextBtn, submitBtn);
        root.setBottom(footer);

        // Listen to answer changes to update progress label live
        choiceGroup.selectedToggleProperty().addListener((obs, o, n) -> updateProgress(progressText));

        // ── Countdown timer ───────────────────────────────────────────────────
        int[] secondsLeft = {exam.durationMinutes * 60};
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsLeft[0]--;
            timerText.setText(formatTime(secondsLeft[0]));

            // Warn when ≤ 5 minutes remain
            if (secondsLeft[0] <= 300) {
                timerText.getStyleClass().add("timer-warn");
            }
            if (secondsLeft[0] <= 60) {
                timerText.getStyleClass().add("timer-danger");
            }

            if (secondsLeft[0] <= 0) {
                countdownTimeline.stop();
                //submitAnswers("TIME_ENDED");
                confirmAndSubmit();
            }
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    public Node getRoot() {
        return root;
    }

    /**
     * Called by StudentApplication when STOP_EXAM arrives or window is closing.
     * Submits once and stops the timer.
     */
    public void forceSubmit() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        if (!isExamActive.getAndSet(false)) {
            return;  // Already ended, prevent double submission
        }

        submitAnswers("TUTOR_STOPPED");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void buildNavButtons() {
        Text navTitle = new Text("Questions");
        navTitle.getStyleClass().add("nav-title");
        navPanel.getChildren().add(navTitle);

        for (int i = 0; i < exam.questions.size(); i++) {
            final int idx = i;
            Button btn = new Button(String.valueOf(i + 1));
            btn.getStyleClass().add("nav-btn");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> showQuestion(idx));
            navButtons[i] = btn;
            navPanel.getChildren().add(btn);
        }
    }

    /**
     * Display the question at the given index.
     *
     * FIX: Properly detach toggle buttons from group before clearing to avoid
     * ConcurrentModificationException when iterating over group's toggle list.
     */
    private void showQuestion(int index) {
        if (index < 0 || index >= exam.questions.size()) {
            return;
        }

        // Save current selection before switching
        saveCurrentSelection();

        currentIndex = index;
        ExamData.Question q = exam.questions.get(index);

        // Highlight current nav button
        for (int i = 0; i < navButtons.length; i++) {
            navButtons[i].getStyleClass().remove("nav-btn-active");
        }
        navButtons[index].getStyleClass().add("nav-btn-active");

        // Build question content
        questionArea.getChildren().clear();

        Text qNum = new Text("Question " + (index + 1) + " of " + exam.questions.size());
        qNum.getStyleClass().add("q-number");

        Text qText = new Text(q.text);
        qText.getStyleClass().add("q-text");
        qText.setWrappingWidth(600);

        VBox choicesBox = new VBox(12);
        choicesBox.getStyleClass().add("choices-box");

        // ── FIX: Properly detach RadioButtons before clearing group ────────────
        // Create a copy of the toggle list before iterating, to avoid
        // ConcurrentModificationException when detaching buttons
        List<Toggle> togglesCopy = new ArrayList<>(choiceGroup.getToggles());
        for (Toggle toggle : togglesCopy) {
            if (toggle instanceof RadioButton) {
                ((RadioButton) toggle).setToggleGroup(null);
            }
        }
        choiceGroup.getToggles().clear();

        Integer savedChoice = answers.get(q.questionId);

        String[] labels = {"A", "B", "C", "D"};
        for (ExamData.Choice choice : q.choices) {
            HBox row = new HBox(12);
            row.getStyleClass().add("choice-row");
            row.setAlignment(Pos.CENTER_LEFT);

            Label badge = new Label(labels[Math.min(choice.choiceId, 3)]);
            badge.getStyleClass().add("choice-badge");

            RadioButton rb = new RadioButton(choice.text);
            rb.getStyleClass().add("choice-radio");
            rb.setToggleGroup(choiceGroup);
            rb.setUserData(choice.choiceId);

            if (savedChoice != null && savedChoice == choice.choiceId) {
                rb.setSelected(true);
            }

            // On selection: record answer and send ANSWER_CHANGE
            rb.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected && isExamActive.get()) {
                    answers.put(q.questionId, choice.choiceId);
                    updateNavButtonStyle(index);
                    submitAnswers("ANSWER_CHANGE");
                }
            });

            row.getChildren().addAll(badge, rb);
            choicesBox.getChildren().add(row);
        }

        questionArea.getChildren().addAll(qNum, qText, choicesBox);
    }

    private void saveCurrentSelection() {
        if (currentIndex < 0 || currentIndex >= exam.questions.size()) {
            return;
        }
        Toggle sel = choiceGroup.getSelectedToggle();
        if (sel != null) {
            long qId = exam.questions.get(currentIndex).questionId;
            answers.put(qId, (Integer) sel.getUserData());
        }
    }

    private void navigate(int delta) {
        if (!isExamActive.get()) {
            return;  // Prevent navigation after exam ends
        }
        saveCurrentSelection();
        showQuestion(currentIndex + delta);
    }

    private void updateNavButtonStyle(int index) {
        if (index < 0 || index >= navButtons.length) {
            return;
        }
        ExamData.Question q = exam.questions.get(index);
        Integer ans = answers.get(q.questionId);
        if (ans != null && ans >= 0) {
            navButtons[index].getStyleClass().add("nav-btn-answered");
        } else {
            navButtons[index].getStyleClass().remove("nav-btn-answered");
        }
    }

    private void updateProgress(Text progressText) {
        long answered = answers.values().stream().filter(v -> v >= 0).count();
        progressText.setText(answered + " / " + exam.questions.size() + " answered");
    }

    private void confirmAndSubmit() {
//        endExam("TIME_ENDED");

        countdownTimeline.stop();
        submitAnswers("TIME_ENDED");
        studentApplication.stopExam(true);
    }

    /**
     * End the exam, submit answers, and mark as inactive.
     *
     * FIX: Properly ends the exam to allow cleanup by StudentApplication
     */
    private void endExam(String trigger) {
        if (!isExamActive.getAndSet(false)) {
            return;  // Already ended
        }

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        submitAnswers(trigger);
    }

    private void submitAnswers(String trigger) {
        if (!isExamActive.get()) {
            wsService.clearStudentName();
            return;  // Exam already ended, don't send more answers
        }

        // For ANSWER_CHANGE, allow repeated sends
        // For TIME_ENDED or TUTOR_STOPPED, only send once
        if (!"ANSWER_CHANGE".equals(trigger)) {
            if (!submitted.compareAndSet(false, true)) {
                wsService.clearStudentName();
                return;  // Already submitted final answers
            }
        }

        // Build a clean map excluding unanswered
        Map<Long, Integer> toSend = new HashMap<>();
        answers.forEach((qId, choiceId) -> {
            if (choiceId >= 0) {
                toSend.put(qId, choiceId);
            }
        });

        wsService.submitAnswers(exam.examId, toSend, trigger);

        if (!"ANSWER_CHANGE".equals(trigger))
            wsService.clearStudentName();
     }

    private static String formatTime(int totalSeconds) {
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}