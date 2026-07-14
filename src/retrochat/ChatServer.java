package retrochat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChatServer {
    private static final int PORT = 5050;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        new ChatServer().start();
    }

    private void start() {
        System.out.println("RetroChat server online on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler, "client-" + socket.getPort()).start();
            }
        } catch (IOException error) {
            System.err.println("Server stopped: " + error.getMessage());
        }
    }

    private void broadcast(String line) {
        System.out.println(line);
        for (ClientHandler client : clients) {
            client.send(line);
        }
    }

    private void broadcastUsers() {
        String names = clients.stream()
            .map(client -> client.username)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.joining(","));
        broadcast("USERS|" + names);
    }

    private String stamp() {
        return LocalTime.now().format(CLOCK);
    }

    private final class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private String username = "guest";
        private boolean joined;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("SYS|" + stamp() + "|Connection accepted. Type /quit to leave.");

                String firstLine = in.readLine();
                if (firstLine == null || !firstLine.startsWith("JOIN|")) {
                    return;
                }

                username = cleanName(firstLine.substring("JOIN|".length()));
                joined = true;
                clients.add(this);
                broadcast("SYS|" + stamp() + "|" + username + " joined the lobby.");
                broadcastUsers();

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equalsIgnoreCase("/quit")) {
                        break;
                    }
                    if (!line.isBlank()) {
                        broadcast("MSG|" + stamp() + "|" + username + "|" + line);
                    }
                }
            } catch (IOException error) {
                System.err.println(username + " disconnected: " + error.getMessage());
            } finally {
                if (joined) {
                    clients.remove(this);
                    broadcast("SYS|" + stamp() + "|" + username + " left the lobby.");
                    broadcastUsers();
                }
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        private void send(String line) {
            if (out != null) {
                out.println(line);
            }
        }

        private String cleanName(String rawName) {
            String cleaned = rawName.trim().replace("|", "").replace(",", "");
            if (cleaned.isBlank()) {
                return "guest" + socket.getPort();
            }
            return cleaned.length() > 16 ? cleaned.substring(0, 16) : cleaned;
        }
    }
}
