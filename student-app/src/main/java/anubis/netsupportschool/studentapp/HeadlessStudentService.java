package anubis.netsupportschool.studentapp;

import anubis.netsupportschool.studentapp.service.UDPDiscoveryService;
import anubis.netsupportschool.studentapp.service.WebSocketService;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Headless mode of the Student Application for Windows Service operation.
 *
 * Runs all core services (UDP discovery, WebSocket) without the JavaFX UI.
 * Useful for:
 *   - Running on servers without display/GPU
 *   - Running as a Windows Service
 *   - Headless exam delivery in lab environments
 *
 * The application maintains full functionality:
 *   - Listens for tutor broadcasts via UDP
 *   - Connects to the backend via WebSocket
 *   - Submits exam answers
 *   - Can be controlled remotely by the tutor
 *
 * Note: Since there's no GUI, lock screen and exam UI are disabled.
 * The tutor can still lock/unlock the computer via other mechanisms (e.g., RDP, Group Policy).
 */
public class HeadlessStudentService {

    private static final Logger log = Logger.getLogger(HeadlessStudentService.class.getName());

    private WebSocketService wsService;
    private UDPDiscoveryService udpService;
    private HeadlessCallbacks callbacks;

    public HeadlessStudentService() {
        // Setup logging for headless mode
        setupLogging();
    }

    /**
     * Start the headless student service.
     */
    public void start() {
        log.info("═══════════════════════════════════════════════════");
        log.info("NetSupport School Student App - Service Mode");
        log.info("═══════════════════════════════════════════════════");

        try {
            // Create callback handler for service events
            callbacks = new HeadlessCallbacks();

            // Initialize services
            HeadlessStudentApplication app = new HeadlessStudentApplication(callbacks);
            wsService = new WebSocketService(app);
            udpService = new UDPDiscoveryService(wsService);

            // Start UDP listener
            log.info("Starting UDP discovery listener on port 9999");
            udpService.startListening();

            log.info("Service started successfully");
            log.info("Waiting for tutor broadcast...");

            // Keep the service alive
            keepAlive();

        } catch (Exception e) {
            log.log(Level.SEVERE, "Service startup failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Stop the headless service.
     */
    public void stop() {
        log.info("Stopping headless service");

        if (udpService != null) {
            udpService.stop();
        }
        if (wsService != null) {
            wsService.disconnect();
        }

        log.info("Service stopped");
    }

    /**
     * Keep the service alive indefinitely.
     * Responds to shutdown signals gracefully.
     */
    private void keepAlive() {
        // Register shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received");
            stop();
        }));

        // Keep the main thread alive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.log(Level.INFO, "Service interrupted", e);
        }
    }

    /**
     * Setup logging for headless mode.
     * Logs to both console and file.
     */
    private void setupLogging() {
        try {
            // File handler for logs
            java.util.logging.FileHandler fh = new java.util.logging.FileHandler(
                    System.getProperty("user.home") + "/AppData/Local/NetSupport/student-app.log",
                    true  // append
            );
            fh.setFormatter(new java.util.logging.SimpleFormatter());
            Logger.getLogger("anubis.netsupportschool.studentapp").addHandler(fh);
            Logger.getLogger("anubis.netsupportschool.studentapp").setLevel(Level.INFO);
        } catch (Exception e) {
            System.err.println("Warning: Could not setup file logging: " + e.getMessage());
        }
    }

    /**
     * Callback handler for service events.
     * In headless mode, these events are logged but don't show UI.
     */
    public static class HeadlessCallbacks {
        HeadlessCallbacks() {
            log.info("Headless callbacks initialized");
        }

        void onLock() {
            log.info("LOCK received from tutor (no GUI - computer should be locked externally)");
        }

        void onUnlock() {
            log.info("UNLOCK received from tutor (no GUI - computer should be unlocked externally)");
        }

        void onExamStart(String examTitle) {
            log.info("EXAM STARTED: " + examTitle);
        }

        void onExamStop() {
            log.info("EXAM STOPPED");
        }

        void onStudentNameRequired() {
            log.info("Student name entry requested (headless mode - using default)");
        }
    }
}