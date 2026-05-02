package anubis.netsupportschool.studentapp;

import anubis.netsupportschool.studentapp.HeadlessStudentService.HeadlessCallbacks;

import java.util.logging.Logger;

/**
 * Minimal adapter that implements StudentApplication interface for headless mode.
 * Replaces the full JavaFX StudentApplication when running as a service.
 *
 * All UI calls are logged instead of displayed.
 */
public class HeadlessStudentApplication extends StudentApplication {

    private static final Logger log = Logger.getLogger(HeadlessStudentApplication.class.getName());
    private final HeadlessCallbacks callbacks;

    public HeadlessStudentApplication(HeadlessCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void onRegistered() {
        log.info("✓ Registered with server");
    }

    public void showLock() {
        log.info("🔒 Lock command received from tutor");
        if (callbacks != null) callbacks.onLock();
    }

    public void hideLock() {
        log.info("🔓 Unlock command received from tutor");
        if (callbacks != null) callbacks.onUnlock();
    }

    public void startExam(ExamData examData) {
        log.info("▶ Starting exam: " + examData.title);
        log.info("   Duration: " + examData.durationMinutes + " minutes");
        log.info("   Questions: " + examData.questions.size());
        if (callbacks != null) callbacks.onExamStart(examData.title);
    }

    public void stopExam(boolean unlock) {
        log.info("■ Exam stopped");
        if (callbacks != null) callbacks.onExamStop();
    }

    public void testLogin() {
        log.info("📋 Test login requested (name entry screen in GUI mode)");
        if (callbacks != null) callbacks.onStudentNameRequired();
    }
}