package anubis.netsupportschool.studentapp;

import anubis.netsupportschool.studentapp.service.UDPDiscoveryService;
import anubis.netsupportschool.studentapp.service.WebSocketService;
import anubis.netsupportschool.studentapp.ui.ExamView;
import anubis.netsupportschool.studentapp.ui.LockScreen;
import anubis.netsupportschool.studentapp.ui.NameEntryView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.util.logging.Logger;

/**
 * Main JavaFX Application.
 *
 * Lifecycle (mirrors the diagram in the spec):
 *   Boot → Windows Service starts → connects via WebSocket → registers → idle
 *   On LOCK        → show fullscreen lock window
 *   On UNLOCK      → destroy lock window, restore desktop
 *   On START_EXAM  → show exam UI inside the lock window
 *   On STOP_EXAM   → close exam, submit answers, show plain lock or unlock
 *
 * FIXED: Proper cleanup of ExamView when exam ends
 */
public class StudentApplication extends Application {

    private static final Logger log = Logger.getLogger(StudentApplication.class.getName());

    // Singleton-style access so services can call back into UI layer
    private static StudentApplication instance;

    private Stage lockStage;
    private LockScreen lockScreen;
    private ExamView examView;

    private WebSocketService wsService;
    private UDPDiscoveryService udpService;

    // ── Application startup ──────────────────────────────────────────────────

    @Override
    public void start(Stage primaryStage) throws InterruptedException {
        instance = this;

        // Keep JavaFX alive even when no windows are visible
        Platform.setImplicitExit(false);

        // Create services
        wsService = new WebSocketService(this);
        udpService = new UDPDiscoveryService(wsService);

        // Start UDP listener – waits for the Tutor to broadcast TUTOR_HERE
        udpService.startListening();
        log.info("Student app started. Listening for tutor broadcast…");
    }

    // ── Public API called by WebSocketService ────────────────────────────────

    /** Called after WebSocket connects and REGISTER handshake is done. */
    public void onRegistered() {
        log.info("Registered with server.");
        // Nothing visible yet – just idle
    }

    /** Tutor sent LOCK → show/bring-forward the fullscreen lock window. */
    public void showLock() {
        Platform.runLater(() -> {
                    if (lockStage == null) {
                        createLockStage();
                    }
                    lockScreen.showPlainLock();
                    lockStage.show();
                    lockStage.toFront();

                    log.info("Lock screen shown.");
                }
        );
    }

    /** Tutor sent UNLOCK → destroy the lock window entirely. */
    public void hideLock() {
        Platform.runLater(() -> {
            if (lockStage != null) {

                if(examView != null && examView.isExamActive()){
                    lockScreen.showContent(examView.getRoot());
                    lockStage.toFront();
                    return;
                }

                lockStage.hide();
                lockStage.close();
                lockStage = null;
                lockScreen = null;
                examView = null;
            }
            log.info("Lock screen hidden.");
        });
    }

    /**
     * Tutor sent START_EXAM → transition lock screen into exam mode.
     *
     * @param examData  parsed exam payload from WebSocketService
     */
    public void startExam(ExamData examData) {
        Platform.runLater(() -> {
            // Ensure the lock stage exists
            if (lockStage == null) {
                createLockStage();
                lockStage.show();
            }

            if (examData.requireNameEntry && (wsService.getStudentName() == null
                    || wsService.getStudentName().startsWith("Student "))) {
                // Show name-entry screen first
                NameEntryView nameEntry = new NameEntryView(name -> {
                    wsService.sendStudentName(name);
                    showExamInLock(examData);
                });
                lockScreen.showContent(nameEntry.getRoot());
            } else {
                showExamInLock(examData);
            }
        });
    }

    /**
     * Exam ended (STOP_EXAM or timer) – submit and return to plain lock or unlock.
     *
     * FIXED: Properly cleans up the exam view and replaces it with lock screen content.
     *
     * @param unlock  true → unlock the computer entirely; false → show plain lock screen
     */
    public void stopExam(boolean unlock) {
        Platform.runLater(() -> {
            // Step 1: Force-submit the exam (marks it as inactive, stops timer)
            if (examView != null) {
                examView.forceSubmit();
            }

            // Step 2: Remove the exam view from scene graph
            if (unlock) {
                // UNLOCK: completely close the lock screen
                hideLock();
            } else {
                // LOCK: replace exam with plain lock screen
                if (lockScreen != null) {
                    lockScreen.showPlainLock();  // This replaces the center content
                }
            }

            // Step 3: Clear the exam reference
            examView = null;

            log.info("Exam stopped. Unlock: " + unlock);
        });
    }

    public void restartDiscovery() {
        Platform.runLater(() -> {
            // Hide any open lock screen
            hideLock();

            // Restart UDP listener so student reconnects fresh
            // with default name when tutor broadcasts again
            if (udpService != null) udpService.stop();
            udpService = new UDPDiscoveryService(wsService);
            udpService.startListening();

            log.info("Discovery restarted — waiting for tutor broadcast.");
        });
    }

    // ── Package-private helpers ──────────────────────────────────────────────

    WebSocketService getWsService() {
        return wsService;
    }

    static StudentApplication getInstance() {
        return instance;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void createLockStage() {
        lockStage = new Stage(StageStyle.UNDECORATED);
        lockScreen = new LockScreen();

        Rectangle2D bounds = Screen.getPrimary().getBounds();
        Scene scene = new Scene(lockScreen.getRoot(), bounds.getWidth(), bounds.getHeight());
        scene.setFill(Color.BLACK);
        String cssPath = getClass().getResource("/anubis/netsupportschool/studentapp/ui/app.css").toExternalForm();
        scene.getStylesheets().add(
                cssPath
        );


        lockStage.setScene(scene);
        lockStage.setFullScreen(true);
        lockStage.setFullScreenExitHint("");          // hide "Press ESC" hint
        lockStage.setAlwaysOnTop(true);

        // Prevent user from closing or minimizing
        lockStage.setOnCloseRequest(e -> e.consume());
    }

    public void testLogin() {
        Platform.runLater(() -> {
            // Ensure the lock stage exists
            if (lockStage == null) {
                createLockStage();
                lockStage.show();
            }

            // Show name-entry screen first
            NameEntryView nameEntry = new NameEntryView(name -> {
                wsService.sendStudentName(name);
                hideLock();
            });
            lockScreen.showContent(nameEntry.getRoot());
        });
    }

    private void showExamInLock(ExamData examData) {
        examView = new ExamView(examData, wsService, this);
        lockScreen.showContent(examView.getRoot());
        lockStage.toFront();
    }

    // ── Shutdown ─────────────────────────────────────────────────────────────

    @Override
    public void stop() {
        if (wsService != null) {
            wsService.disconnect();
        }
        if (udpService != null) {
            udpService.stop();
        }
    }
}