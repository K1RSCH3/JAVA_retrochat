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

// Runs the shared lobby and passes messages between clients
public class ChatServer {
    // Clients use this port to reach the server
    private static final int PORT = 5050;
    // Show message times as hours and minutes
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    // Keeps the client list safe to use across threads
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    // Starts the server app
    public static void main(String[] args) {
        new ChatServer().start();
    }

    // Keep waiting for new clients to connect
    private void start() {
        System.out.println("RetroChat server online on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Wait here until another client connects
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                // Each client gets its own thread
                new Thread(handler, "client-" + socket.getPort()).start();
            }
        } catch (IOException error) {
            System.err.println("Server stopped: " + error.getMessage());
        }
    }

    // Send to everyone, including whoever sent the message
    private void broadcast(String line) {
        // Keep a copy in the server console too
        System.out.println(line);
        for (ClientHandler client : clients) {
            client.send(line);
        }
    }

    // Update the online list when someone joins or leaves
    private void broadcastUsers() {
        // Sort the names and pack them into one comma-separated line
        String names = clients.stream()
            .map(client -> client.username)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.joining(","));
        broadcast("USERS|" + names);
    }

    // Get the current server time for a message
    private String stamp() {
        return LocalTime.now().format(CLOCK);
    }

    // Handles one client's connection and messages
    private final class ClientHandler implements Runnable {
        // The connection to this client and the writer for sending replies
        private final Socket socket;
        private PrintWriter out;
        // Use a temporary name until the client joins
        private String username = "guest";
        // Track whether this client actually made it into the lobby
        private boolean joined;

        // Save the connection this handler will look after
        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        // Let the client join, then read their messages until they leave
        @Override
        public void run() {
            // Read text from this client and close the reader when we're done
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                // Send each line right away
                out = new PrintWriter(socket.getOutputStream(), true);
                // Notice format: SYS|time|notice
                out.println("SYS|" + stamp() + "|Connection accepted. Type /quit to leave.");

                // Wait for JOIN|name before letting the client in
                String firstLine = in.readLine();
                if (firstLine == null || !firstLine.startsWith("JOIN|")) {
                    // Stop if they disconnected or didn't send a join request
                    return;
                }

                // Save their name and let everyone know they joined
                username = cleanName(firstLine.substring("JOIN|".length()));
                joined = true;
                clients.add(this);
                broadcast("SYS|" + stamp() + "|" + username + " joined the lobby.");
                broadcastUsers();

                // Read one message at a time until the connection closes
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equalsIgnoreCase("/quit")) {
                        // Leave the loop so the cleanup below can run
                        break;
                    }
                    // Skip messages that are just spaces
                    if (!line.isBlank()) {
                        // Message format: MSG|time|username|message
                        broadcast("MSG|" + stamp() + "|" + username + "|" + line);
                    }
                }
            } catch (IOException error) {
                System.err.println(username + " disconnected: " + error.getMessage());
            } finally {
                // Clean up when the client leaves or loses connection
                if (joined) {
                    clients.remove(this);
                    broadcast("SYS|" + stamp() + "|" + username + " left the lobby.");
                    broadcastUsers();
                }
                try {
                    // Close the connection even if the client never joined
                    socket.close();
                } catch (IOException ignored) {
                    // Client is already removed from the lobby
                }
            }
        }

        // Send one line to this client once the writer is ready
        private void send(String line) {
            if (out != null) {
                out.println(line);
            }
        }

        // Tidy up the name and keep it within 16 characters
        private String cleanName(String rawName) {
            // Remove | and , so the name won't mess up the message format
            String cleaned = rawName.trim().replace("|", "").replace(",", "");
            if (cleaned.isBlank()) {
                // Use the connection's port to give an unnamed guest a name
                return "guest" + socket.getPort();
            }
            return cleaned.length() > 16 ? cleaned.substring(0, 16) : cleaned;
        }
    }
}
