package com.qtpc.tech.nolmax.server;

import com.qtpc.tech.nolmax.server.configuration.AppConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatServerTest {

    @Test
    void bindsToConfiguredAddressAndStopsWhenStartupThreadIsInterrupted() throws Exception {
        int port = findAvailablePort();
        AppConfig config = new AppConfig();
        config.server = new AppConfig.Server();
        config.server.listen_address = "127.0.0.1";
        config.server.port = port;

        Thread serverThread = new Thread(() -> new ChatServer(config).start(), "chat-server-test");
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            assertTrue(awaitServer(port, serverThread), "server did not start listening on its configured port");
        } finally {
            serverThread.interrupt();
            serverThread.join(TimeUnit.SECONDS.toMillis(5));
        }

        assertFalse(serverThread.isAlive(), "server did not stop after its startup thread was interrupted");
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static boolean awaitServer(int port, Thread serverThread) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (serverThread.isAlive() && System.nanoTime() < deadline) {
            try (Socket client = new Socket()) {
                client.connect(new InetSocketAddress("127.0.0.1", port), 100);
                return true;
            } catch (IOException ignored) {
                Thread.sleep(25);
            }
        }
        return false;
    }
}
