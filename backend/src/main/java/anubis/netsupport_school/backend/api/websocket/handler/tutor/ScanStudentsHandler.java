package anubis.netsupport_school.backend.api.websocket.handler.tutor;

import anubis.netsupport_school.backend.api.websocket.UDPBroadcaster;
import anubis.netsupport_school.backend.api.websocket.handler.MessageHandler;
import anubis.netsupport_school.backend.domain.dto.websocket.ScanStudentsMessage;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@Component
public class ScanStudentsHandler implements MessageHandler<ScanStudentsMessage> {
    private final UDPBroadcaster broadcaster;
    private final TaskExecutor taskExecutor;
    private final ServletWebServerApplicationContext serverContext;

    public ScanStudentsHandler(UDPBroadcaster broadcaster,
                               TaskExecutor taskExecutor,
                               ServletWebServerApplicationContext serverContext) {
        this.broadcaster = broadcaster;
        this.taskExecutor = taskExecutor;
        this.serverContext = serverContext;
    }

    @Override
    public void handle(WebSocketSession session, ScanStudentsMessage message) throws IOException {
        String ip = getActualLanIP();
        int port = serverContext.getWebServer().getPort();

        // Start the scan; if it's already running, startScan returns false
        if (broadcaster.startScan(ip, port)) {
            taskExecutor.execute(broadcaster);
            System.out.println("Broadcast initiated on " + ip + ":" + port);
        } else {
            System.out.println("Scan request ignored - broadcast already in progress.");
        }
    }

    private String getActualLanIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;

                // Extra check: skip known virtual interface prefixes
                String name = iface.getName().toLowerCase();
                if (name.contains("vbox") || name.contains("vmnet") || name.contains("docker")) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            return "127.0.0.1";
        }
        return "127.0.0.1";
    }

    @Override
    public Class<ScanStudentsMessage> getMessageType() {
        return ScanStudentsMessage.class;
    }
}
