package anubis.netsupportschool.studentapp.service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens on UDP port 9999 for a TUTOR_HERE broadcast.
 *
 * Expected payload (from backend UDPBroadcaster):
 *   {"type":"TUTOR_HERE","serverIp":"192.168.1.x","serverPort":8080}
 *
 * On receipt, hands the server coordinates to WebSocketService which
 * then opens the WebSocket connection.
 *
 * OPTIMIZATIONS:
 *   - No artificial timeout polling (blocking receive)
 *   - Proper socket lifecycle management
 *   - Thread interruption support
 *   - Efficient buffer handling
 *   - Better exception handling with specificity
 */
public class UDPDiscoveryService {

    private static final Logger log = Logger.getLogger(UDPDiscoveryService.class.getName());
    private static final int    UDP_PORT    = 9999;
    private static final int    BUFFER_SIZE = 512;
    private static final int    THREAD_STOP_TIMEOUT = 5000;  // 5 seconds

    // Simple regex – avoids pulling in a JSON library
    private static final Pattern IP_PATTERN   = Pattern.compile("\"serverIp\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PORT_PATTERN = Pattern.compile("\"serverPort\"\\s*:\\s*(\\d+)");

    private final WebSocketService wsService;
    private volatile boolean running = false;
    private Thread listenerThread;
    private DatagramSocket socket;

    public UDPDiscoveryService(WebSocketService wsService) {
        this.wsService = wsService;
    }

    /**
     * Start listening for UDP broadcasts.
     * Safe to call multiple times - will not create duplicate listeners.
     */
    public void startListening() {
        synchronized (this) {
            if (running || (listenerThread != null && listenerThread.isAlive())) {
                return;  // Already running
            }

            running = true;
            listenerThread = new Thread(this::listenLoop, "udp-discovery");
            listenerThread.setDaemon(true);
            listenerThread.start();
            log.info("UDP discovery listening on port " + UDP_PORT);
        }
    }

    /**
     * Stop listening and clean up resources.
     * Blocks until the listener thread terminates or timeout expires.
     */
    public void stop() {
        running = false;

        // Close socket to interrupt any blocking receive()
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        // Wait for thread to finish gracefully
        if (listenerThread != null && listenerThread.isAlive()) {
            try {
                listenerThread.join(THREAD_STOP_TIMEOUT);
                if (listenerThread.isAlive()) {
                    listenerThread.interrupt();
                    log.warning("UDP listener thread did not terminate within "
                            + THREAD_STOP_TIMEOUT + "ms");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warning("Interrupted while waiting for UDP listener to stop");
            }
        }

        log.info("UDP discovery stopped");
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Main listening loop.
     *
     * OPTIMIZATION BENEFITS:
     *   1. No timeout polling → CPU efficient
     *   2. Blocks on receive() → wakes naturally when socket closes
     *   3. Proper exception handling → specific error logging
     *   4. Thread-safe shutdown → respects running flag and interruption
     *   5. Buffer management → only processes received bytes
     */
    private void listenLoop() {
        DatagramSocket localSocket = null;
        try {
            // Create socket with SO_REUSEADDR for quick rebinding
            localSocket = new DatagramSocket(UDP_PORT);
            localSocket.setReuseAddress(true);
            this.socket = localSocket;

            log.fine("UDP socket created on port " + UDP_PORT);
            byte[] buf = new byte[BUFFER_SIZE];

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    // Blocking call - naturally wakes when:
                    //   - Data arrives
                    //   - Socket is closed (throws SocketException)
                    //   - Thread is interrupted
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    localSocket.receive(packet);

                    // Parse only the received bytes, not the entire buffer
                    String msg = new String(
                            buf,
                            0,
                            packet.getLength(),
                            StandardCharsets.UTF_8
                    );

                    log.info("UDP received: " + msg);
                    handleBroadcast(msg);

                } catch (SocketException e) {
                    // Socket closed - expected during shutdown
                    if (running) {
                        log.warning("UDP socket error: " + e.getMessage());
                    }
                    break;
                }
            }

        } catch (SocketException e) {
            // Failed to bind to port
            if (running) {
                log.severe("Failed to create UDP socket on port " + UDP_PORT
                        + ": " + e.getMessage());
            }
        } catch (Exception e) {
            if (running) {
                log.severe("UDP listener error [" + e.getClass().getSimpleName() + "]: "
                        + e.getMessage());
            }
        } finally {
            // Cleanup resources
            if (localSocket != null && !localSocket.isClosed()) {
                localSocket.close();
            }
            this.socket = null;
            running = false;
            log.fine("UDP listener loop terminated");
        }
    }

    /**
     * Handle incoming broadcast message.
     *
     * Extracts IP and port from JSON payload and initiates WebSocket connection.
     * Includes validation to prevent invalid connections.
     */
    private void handleBroadcast(String json) {
        if (json == null || json.isEmpty()) {
            return;
        }

        // Only process TUTOR_HERE messages
        if (!json.contains("TUTOR_HERE")) {
            log.fine("Ignoring non-TUTOR_HERE message");
            return;
        }

        try {
            Matcher ipMatcher   = IP_PATTERN.matcher(json);
            Matcher portMatcher = PORT_PATTERN.matcher(json);

            if (!ipMatcher.find() || !portMatcher.find()) {
                log.fine("TUTOR_HERE broadcast missing IP or port fields");
                return;
            }

            String ip = ipMatcher.group(1).trim();
            String portStr = portMatcher.group(1);

            // Validate IP not empty
            if (ip.isEmpty()) {
                log.warning("Tutor IP is empty");
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                log.warning("Invalid port number in broadcast: " + portStr);
                return;
            }

            // Validate port range
            if (port < 1 || port > 65535) {
                log.warning("Port out of valid range [1-65535]: " + port);
                return;
            }

            log.info("Tutor discovered at " + ip + ":" + port);
            wsService.connect(ip, port);

        } catch (Exception e) {
            log.warning("Error parsing broadcast: " + e.getMessage());
        }
    }
}