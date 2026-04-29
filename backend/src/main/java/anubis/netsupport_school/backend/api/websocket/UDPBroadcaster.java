package anubis.netsupport_school.backend.api.websocket;

import org.springframework.stereotype.Component;

import java.net.*;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class UDPBroadcaster implements Runnable {
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private String serverIp;
    private int serverPort;

    private static final int NUMBER_OF_BROADCASTS = 3;
    private static final long INTERVAL = 750;
    private static final int TARGET_PORT = 9999;

    public boolean startScan(String ip, int port) {
        // compareAndSet(expected, update) returns true only if it was false
        if (isScanning.compareAndSet(false, true)) {
            this.serverIp = ip;
            this.serverPort = port;
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
//            socket.setBroadcast(true);
//            InetAddress broadcastAddr = getBroadcastAddress();
//
//            for (int i = 0; i < NUMBER_OF_BROADCASTS; i++) {
//                String message = String.format(
//                        "{\"type\":\"TUTOR_HERE\",\"serverIp\":\"%s\",\"serverPort\":%d}",
//                        serverIp, serverPort
//                );
//                byte[] data = message.getBytes();
//                DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddr, TARGET_PORT);
//                socket.send(packet);
//                Thread.sleep(INTERVAL);
//            }

            socket.setBroadcast(true);

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;

                for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                    InetAddress broadcast = addr.getBroadcast();
                    if (broadcast == null) continue;

                    for (int i = 0; i < NUMBER_OF_BROADCASTS; i++) {
                        String message = String.format(
                                "{\"type\":\"TUTOR_HERE\",\"serverIp\":\"%s\",\"serverPort\":%d}",
                                serverIp, serverPort
                        );
                        byte[] data = message.getBytes();
                        DatagramPacket packet = new DatagramPacket(data, data.length, broadcast, TARGET_PORT);
                        socket.send(packet);
                        Thread.sleep(INTERVAL);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isScanning.set(false);
        }
    }

    private InetAddress getBroadcastAddress() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;

            for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                if (addr.getBroadcast() != null) return addr.getBroadcast();
            }
        }
        try { return InetAddress.getByName("255.255.255.255"); } catch (Exception e) { return null; }
    }
}
