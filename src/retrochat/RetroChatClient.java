package retrochat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

// The chat window that connects to the server and shows the conversation
public class RetroChatClient {
    // Colors for the window, message text, and server notices
    private static final Color NES_BLUE = new Color(37, 94, 170);
    private static final Color NES_CYAN = new Color(0, 188, 212);
    private static final Color NES_PINK = new Color(236, 0, 140);
    private static final Color NES_GREEN = new Color(0, 232, 120);
    private static final Color INK = new Color(24, 24, 24);
    private static final Color PLACEHOLDER_GRAY = new Color(145, 145, 145);
    private static final Color TIME_GRAY = new Color(96, 96, 96);
    private static final Color SERVER_PURPLE = new Color(112, 48, 160);
    // Pick from these colors when showing usernames
    private static final Color[] USER_COLORS = {
        new Color(0, 92, 210),
        new Color(204, 0, 0),
        new Color(0, 130, 72),
        new Color(210, 104, 0),
        new Color(132, 0, 180),
        new Color(0, 132, 132),
        new Color(184, 0, 104),
        new Color(92, 92, 0)
    };
    // Shared font to give the window a retro look
    private static final Font PIXEL_FONT = new Font("Monospaced", Font.BOLD, 13);

    // Keep the window parts here so other methods can update them
    private JFrame frame;
    // The conversation, online names, and typing controls
    private JTextPane transcript;
    private JTextArea userList;
    private JTextField input;
    private JButton sendButton;
    // Sends our messages to the server
    private PrintWriter out;
    // The name we chose when opening the app
    private String username;

    // Starts the client app
    public static void main(String[] args) {
        // Start the window on Swing's UI thread
        SwingUtilities.invokeLater(() -> new RetroChatClient().start());
    }

    // Ask for connection details, open the window, and join the server
    private void start() {
        username = ask("Choose username", "bubbles");
        String host = ask("Server host", "localhost");
        // Turn the port from the prompt into a number
        int port = Integer.parseInt(ask("Server port", "5050"));

        buildWindow();
        connect(host, port);
    }

    // Show a prompt and use the default if it's empty or cancelled
    private String ask(String title, String fallback) {
        String value = JOptionPane.showInputDialog(null, title, fallback);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    // Put together the chat area, online list, and message controls
    private void buildWindow() {
        // Set up the main window
        frame = new JFrame("RetroChat - " + username);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(760, 480));
        frame.setLayout(new BorderLayout(4, 4));
        frame.getContentPane().setBackground(NES_BLUE);

        // Show the conversation here without letting users edit it
        transcript = new JTextPane();
        transcript.setEditable(false);
        transcript.setFont(PIXEL_FONT);
        transcript.setForeground(INK);
        transcript.setBackground(Color.WHITE);

        // Let the conversation scroll when it gets long
        JScrollPane scrollPane = new JScrollPane(transcript);
        scrollPane.setBorder(BorderFactory.createLineBorder(NES_CYAN, 4));

        // Show our name for now until the server sends the online list
        userList = new JTextArea("ONLINE\n> " + username);
        userList.setEditable(false);
        userList.setFont(PIXEL_FONT);
        userList.setForeground(Color.WHITE);
        userList.setBackground(NES_BLUE.darker());
        userList.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Put the online label and user list in the right sidebar
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(160, 0));
        rightPanel.setBackground(NES_BLUE);
        JLabel statusDot = new JLabel("  * ONLINE");
        statusDot.setFont(PIXEL_FONT);
        statusDot.setForeground(NES_GREEN);
        rightPanel.add(statusDot, BorderLayout.NORTH);
        rightPanel.add(userList, BorderLayout.CENTER);

        // Text box for typing a message
        input = new PlaceholderTextField("Type your message here...");
        input.setFont(PIXEL_FONT);
        input.setForeground(INK);
        input.setBackground(Color.WHITE);
        input.setBorder(BorderFactory.createLineBorder(NES_CYAN, 3));
        // Enter works the same as the SEND button
        input.addActionListener(this::sendMessage);

        // Send whatever is in the text box when clicked
        sendButton = new JButton("SEND");
        sendButton.setFont(PIXEL_FONT);
        sendButton.setForeground(Color.WHITE);
        sendButton.setBackground(NES_PINK);
        sendButton.setFocusPainted(false);
        sendButton.addActionListener(this::sendMessage);

        // Keep the text box and send button together at the bottom
        JPanel bottomPanel = new JPanel(new BorderLayout(4, 0));
        bottomPanel.setBackground(NES_BLUE);
        bottomPanel.add(input, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        // Leave a little space around the chat area
        JPanel centerPanel = new JPanel(new GridLayout(1, 1));
        centerPanel.setBackground(NES_BLUE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 0));
        centerPanel.add(scrollPane);

        // Put all the sections in place and show the window
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(rightPanel, BorderLayout.EAST);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    // Open a connection and start listening for replies
    private void connect(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            // Send our name to join the chat
            out.println("JOIN|" + username);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Listen in a separate thread so the window stays responsive
            new Thread(() -> readMessages(in), "server-listener").start();
        } catch (IOException error) {
            // Turn off sending if we couldn't connect
            appendSystem("--:--", "Could not connect: " + error.getMessage());
            input.setEnabled(false);
            sendButton.setEnabled(false);
        }
    }

    // Keep reading server messages until the connection ends
    private void readMessages(BufferedReader in) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handleServerLine(line);
            }
        } catch (IOException error) {
            appendSystem("--:--", "Disconnected from server.");
        }
    }

    // Check what kind of update the server sent and display it
    private void handleServerLine(String line) {
        // Split at the first 3 pipes and leave any others in the message
        String[] parts = line.split("\\|", 4);
        // SYS = server notice, MSG = chat message, USERS = online list
        if (parts.length >= 3 && parts[0].equals("SYS")) {
            appendSystem(parts[1], parts[2]);
            return;
        }

        if (parts.length == 4 && parts[0].equals("MSG")) {
            appendMessage(parts[1], parts[2], parts[3]);
            return;
        }

        if (parts.length == 2 && parts[0].equals("USERS")) {
            updateUserList(parts[1]);
            return;
        }

        // Show anything we don't recognize as a plain server notice
        appendSystem("--:--", line);
    }

    // Send the text box contents and clear it for the next message
    private void sendMessage(ActionEvent event) {
        String message = input.getText().trim();
        // Skip empty messages or sending before the writer is ready
        if (message.isEmpty() || out == null) {
            return;
        }

        // The server sends this back to us too, so it shows up in our chat
        out.println(message);
        input.setText("");

        // Close our window after telling the server we're leaving
        if (message.equalsIgnoreCase("/quit")) {
            frame.dispose();
        }
    }

    // Add a chat message with its time and sender's name
    private void appendMessage(String time, String sender, String message) {
        // Switch to the UI thread before updating the chat
        SwingUtilities.invokeLater(() -> {
            // Style the time, name, and message separately
            appendSegment("> ", style(INK, false, false));
            appendSegment(time + " ", style(TIME_GRAY, true, false));
            appendSegment(sender + ": ", style(userColor(sender), false, true));
            appendSegment(message + System.lineSeparator(), style(INK, false, false));
            // Scroll down to the newest message
            transcript.setCaretPosition(transcript.getDocument().getLength());
        });
    }

    // Add a server notice with a purple SERVER label
    private void appendSystem(String time, String message) {
        // Server notices need to update the window on the UI thread too
        SwingUtilities.invokeLater(() -> {
            appendSegment("* ", style(INK, false, false));
            appendSegment(time + " ", style(TIME_GRAY, true, false));
            appendSegment("SERVER: ", style(SERVER_PURPLE, false, true));
            appendSegment(message + System.lineSeparator(), style(INK, false, false));
            transcript.setCaretPosition(transcript.getDocument().getLength());
        });
    }

    // Make a text style with the chosen color, italics, and bold setting
    private SimpleAttributeSet style(Color color, boolean italic, boolean bold) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attributes, PIXEL_FONT.getFamily());
        StyleConstants.setFontSize(attributes, PIXEL_FONT.getSize());
        StyleConstants.setForeground(attributes, color);
        StyleConstants.setItalic(attributes, italic);
        StyleConstants.setBold(attributes, bold);
        return attributes;
    }

    // Use the name to pick the same color each time
    private Color userColor(String sender) {
        int index = Math.abs(sender.toLowerCase().hashCode()) % USER_COLORS.length;
        return USER_COLORS[index];
    }

    // Rebuild the sidebar from the comma-separated names the server sent
    private void updateUserList(String packedNames) {
        SwingUtilities.invokeLater(() -> {
            // Start fresh so people who left don't stay in the list
            StringBuilder list = new StringBuilder("ONLINE");
            if (!packedNames.isBlank()) {
                for (String name : packedNames.split(",")) {
                    if (!name.isBlank()) {
                        // Put a star next to our own name
                        String marker = name.equalsIgnoreCase(username) ? "* " : "> ";
                        list.append(System.lineSeparator()).append(marker).append(name);
                    }
                }
            }
            userList.setText(list.toString());
        });
    }

    // Add one styled piece of text to the end of the chat
    private void appendSegment(String text, SimpleAttributeSet attributes) {
        StyledDocument document = transcript.getStyledDocument();
        try {
            document.insertString(document.getLength(), text, attributes);
        } catch (BadLocationException error) {
            throw new IllegalStateException("Could not update chat transcript.", error);
        }
    }

    // A text box that shows a hint while it's empty
    private static final class PlaceholderTextField extends JTextField {
        // The hint is just drawn on screen, so it won't get sent
        private final String placeholder;

        // Save the hint this text box will show
        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        // Draw the normal text box, then add the hint if nothing is typed
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            // Hide the hint as soon as the text box has something in it
            if (!getText().isEmpty()) {
                return;
            }

            // Draw with a copy so we don't change Swing's drawing settings
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            copy.setColor(PLACEHOLDER_GRAY);
            copy.setFont(getFont().deriveFont(Font.ITALIC));
            // Keep the hint near the left edge and centered vertically
            int x = getInsets().left + 4;
            int y = (getHeight() - copy.getFontMetrics().getHeight()) / 2 + copy.getFontMetrics().getAscent();
            copy.drawString(placeholder, x, y);
            // Release the drawing copy when we're done
            copy.dispose();
        }
    }
}
