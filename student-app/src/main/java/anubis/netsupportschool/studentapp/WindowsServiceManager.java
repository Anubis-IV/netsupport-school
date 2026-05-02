package anubis.netsupportschool.studentapp;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Windows Service Management Utility.
 *
 * Uses NSSM (Non-Sucking Service Manager) to install/uninstall the app as a Windows Service.
 * NSSM handles:
 *   - Service installation with auto-start
 *   - Automatic restart on failure
 *   - Graceful shutdown
 *   - Service logging
 *
 * NSSM is lightweight (free, open-source) and perfect for Java applications.
 */
public class WindowsServiceManager {

    private static final Logger log = Logger.getLogger(WindowsServiceManager.class.getName());

    private static final String SERVICE_NAME = "NetSupportStudentApp";
    private static final String SERVICE_DISPLAY_NAME = "NetSupport School Student Application";
    private static final String SERVICE_DESCRIPTION = "Automated exam delivery and monitoring client for NetSupport School";

    /**
     * Install the application as a Windows Service using NSSM.
     *
     * NSSM must be in the system PATH or bundled with the application.
     * Download: https://nssm.cc/download
     */
    public static void installService() {
        log.info("Installing Windows Service: " + SERVICE_NAME);

        if (!isWindows()) {
            System.err.println("❌ Windows Service installation is only available on Windows OS");
            System.exit(1);
        }

        if (!isNssmAvailable()) {
            System.err.println("❌ NSSM not found. Please:");
            System.err.println("   1. Download NSSM from https://nssm.cc/download");
            System.err.println("   2. Extract to C:\\nssm or add to PATH");
            System.exit(1);
        }

        try {
            String jarPath = getJarPath();
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";

            if (!new File(javaBin).exists()) {
                System.err.println("❌ Java executable not found at: " + javaBin);
                System.exit(1);
            }

            // Install service with NSSM
            ProcessBuilder pb = new ProcessBuilder(
                    "nssm",
                    "install",
                    SERVICE_NAME,
                    javaBin,
                    "-jar",
                    jarPath,
                    "--service-mode"
            );

            Process p = pb.start();
            int exitCode = p.waitFor();

            if (exitCode == 0) {
                // Configure service for auto-restart on failure
                configureService();
                System.out.println("✓ Service installed successfully!");
                System.out.println("  Service Name: " + SERVICE_NAME);
                System.out.println("  Start the service with: nssm start " + SERVICE_NAME);
            } else {
                System.err.println("❌ Service installation failed (exit code: " + exitCode + ")");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("❌ Installation error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Uninstall the Windows Service.
     */
    public static void uninstallService() {
        log.info("Uninstalling Windows Service: " + SERVICE_NAME);

        if (!isWindows()) {
            System.err.println("❌ Windows Service management is only available on Windows OS");
            System.exit(1);
        }

        try {
            // Stop service first
            new ProcessBuilder("nssm", "stop", SERVICE_NAME, "kill").start().waitFor();
            Thread.sleep(1000);

            // Remove service
            ProcessBuilder pb = new ProcessBuilder("nssm", "remove", SERVICE_NAME, "confirm");
            Process p = pb.start();
            int exitCode = p.waitFor();

            if (exitCode == 0) {
                System.out.println("✓ Service uninstalled successfully!");
            } else {
                System.err.println("❌ Service uninstall failed (exit code: " + exitCode + ")");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("❌ Uninstall error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Start the Windows Service.
     */
    public static void startService() {
        log.info("Starting Windows Service: " + SERVICE_NAME);

        try {
            ProcessBuilder pb = new ProcessBuilder("nssm", "start", SERVICE_NAME);
            Process p = pb.start();
            int exitCode = p.waitFor();

            if (exitCode == 0) {
                System.out.println("✓ Service started successfully!");
            } else {
                System.err.println("❌ Failed to start service (exit code: " + exitCode + ")");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Stop the Windows Service.
     */
    public static void stopService() {
        log.info("Stopping Windows Service: " + SERVICE_NAME);

        try {
            ProcessBuilder pb = new ProcessBuilder("nssm", "stop", SERVICE_NAME);
            Process p = pb.start();
            int exitCode = p.waitFor();

            if (exitCode == 0) {
                System.out.println("✓ Service stopped successfully!");
            } else {
                System.err.println("❌ Failed to stop service (exit code: " + exitCode + ")");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // ── Helper Methods ────────────────────────────────────────────────────────

    /**
     * Configure service for automatic restart on failure.
     */
    private static void configureService() throws IOException, InterruptedException {
        // Set service to restart if it crashes (exit code 1)
        new ProcessBuilder(
                "nssm",
                "set",
                SERVICE_NAME,
                "AppExit",
                "Default",
                "Restart"
        ).start().waitFor();

        // Set restart delay to 5 seconds
        new ProcessBuilder(
                "nssm",
                "set",
                SERVICE_NAME,
                "AppRestartDelay",
                "5000"
        ).start().waitFor();

        // Set service to start automatically
        new ProcessBuilder(
                "nssm",
                "set",
                SERVICE_NAME,
                "Start",
                "SERVICE_AUTO_START"
        ).start().waitFor();

        // Set service description
        new ProcessBuilder(
                "nssm",
                "set",
                SERVICE_NAME,
                "Description",
                SERVICE_DESCRIPTION
        ).start().waitFor();

        log.info("Service configured for auto-restart");
    }

    /**
     * Get the path to the running JAR file.
     */
    private static String getJarPath() {
        String classPath = System.getProperty("java.class.path");
        String[] paths = classPath.split(File.pathSeparator);

        // Find the first JAR file
        for (String path : paths) {
            if (path.endsWith(".jar")) {
                return new File(path).getAbsolutePath();
            }
        }

        // Fallback: try to find from code source
        String codeSourcePath = HelloApplication.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath();

        return new File(codeSourcePath).getAbsolutePath();
    }

    /**
     * Check if NSSM is available in PATH.
     */
    private static boolean isNssmAvailable() {
        try {
            Process p = new ProcessBuilder("where", "nssm").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            // Try common installation paths
            return new File("C:\\nssm\\nssm.exe").exists() ||
                    new File("C:\\Program Files\\nssm\\nssm.exe").exists() ||
                    new File("C:\\Program Files (x86)\\nssm\\nssm.exe").exists();
        }
    }

    /**
     * Check if running on Windows OS.
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}