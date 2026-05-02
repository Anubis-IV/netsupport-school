package anubis.netsupportschool.studentapp;

import javafx.application.Application;

import java.util.logging.Logger;

/**
 * Application launcher with Windows Service support.
 *
 * Automatically detects if running as a Windows Service and manages
 * the JavaFX application lifecycle accordingly.
 *
 * Usage:
 *   java -jar student-app.jar                        # Normal mode
 *   java -jar student-app.jar --install-service     # Install as service
 *   java -jar student-app.jar --uninstall-service   # Remove service
 */
public class HelloApplication {

    private static final Logger log = Logger.getLogger(HelloApplication.class.getName());

    public static void main(String[] args) {
        // Handle service management commands
        if (args.length > 0) {
            String cmd = args[0].toLowerCase();

            if ("--install-service".equals(cmd)) {
                WindowsServiceManager.installService();
                return;
            } else if ("--uninstall-service".equals(cmd)) {
                WindowsServiceManager.uninstallService();
                return;
            } else if ("--start-service".equals(cmd)) {
                WindowsServiceManager.startService();
                return;
            } else if ("--stop-service".equals(cmd)) {
                WindowsServiceManager.stopService();
                return;
            } else if ("--service-mode".equals(cmd)) {
                // Running as Windows Service - no UI
                runHeadless();
                return;
            }
        }

        // Normal GUI mode
        log.info("Starting NetSupport School Student App (GUI Mode)");
        Application.launch(StudentApplication.class, args);
    }

    /**
     * Headless mode for Windows Service operation.
     * The application runs without GUI but maintains WebSocket connection.
     */
    private static void runHeadless() {
        log.info("Starting NetSupport School Student App (Service Mode - Headless)");
        try {
            // Create a headless instance of the core service components
            // without initializing JavaFX
            HeadlessStudentService service = new HeadlessStudentService();
            service.start();

            // Keep the service running
            Thread.currentThread().join();
        } catch (Exception e) {
            log.severe("Service startup failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}