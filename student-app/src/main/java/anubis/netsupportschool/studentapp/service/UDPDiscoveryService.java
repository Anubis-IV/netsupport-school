package anubis.netsupportschool.studentapp.service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
 */
public class UDPDiscoveryService {

    private static final Logger log = Logger.getLogger(UDPDiscoveryService.class.getName());
    private static final int    UDP_PORT    = 9999;
    private static final int    BUFFER_SIZE = 512;

    // Simple regex – avoids pulling in a JSON library
    private static final Pattern IP_PATTERN   = Pattern.compile("\"serverIp\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PORT_PATTERN = Pattern.compile("\"serverPort\"\\s*:\\s*(\\d+)");

    private final WebSocketService wsService;
    private volatile boolean running = false;
    private Thread listenerThread;

    public UDPDiscoveryService(WebSocketService wsService) {
        this.wsService = wsService;
    }

    public void startListening() {
        running = true;
        listenerThread = new Thread(this::listenLoop, "udp-discovery");
        listenerThread.setDaemon(true);
        listenerThread.start();
        log.info("UDP discovery listening on port " + UDP_PORT);
    }

    public void stop() {
        running = false;
        if (listenerThread != null) listenerThread.interrupt();
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void listenLoop() {
        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
            socket.setSoTimeout(2000); // 2 s timeout so we can check 'running'
            byte[] buf = new byte[BUFFER_SIZE];

            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    log.info("UDP received: " + msg);
                    handleBroadcast(msg);
                } catch (java.net.SocketTimeoutException ignored) {
                    // normal – just loop again
                }
            }
        } catch (Exception e) {
            if (running) log.severe("UDP listener error: " + e.getMessage());
        }
    }

    private void handleBroadcast(String json) {
        if (!json.contains("TUTOR_HERE")) return;

        Matcher ipMatcher   = IP_PATTERN.matcher(json);
        Matcher portMatcher = PORT_PATTERN.matcher(json);

        if (ipMatcher.find() && portMatcher.find()) {
            String ip   = ipMatcher.group(1);
            int    port = Integer.parseInt(portMatcher.group(1));
            log.info("Tutor discovered at " + ip + ":" + port);
            wsService.connect(ip, port);
        }
    }
}
