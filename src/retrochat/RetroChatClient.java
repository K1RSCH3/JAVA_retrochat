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

public class RetroChatClient {
    private static final Color NES_BLUE = new Color(37, 94, 170);
    private static final Color NES_CYAN = new Color(0, 188, 212);
    private static final Color NES_PINK = new Color(236, 0, 140);
    private static final Color NES_GREEN = new Color(0, 232, 120);
    private static final Color INK = new Color(24, 24, 24);
    private static final Color PLACEHOLDER_GRAY = new Color(145, 145, 145);
    private static final Color TIME_GRAY = new Color(96, 96, 96);
    private static final Color SERVER_PURPLE = new Color(112, 48, 160);
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
    private static final Font PIXEL_FONT = new Font("Monospaced", Font.BOLD, 13);

    private JFrame frame;
    private JTextPane transcript;
    private JTextArea userList;
    private JTextField input;
    private JButton sendButton;
    private PrintWriter out;
    private String username;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RetroChatClient().start());
    }

    private void start() {
        username = ask("Choose username", "bubbles");
        String host = ask("Server host", "localhost");
        int port = Integer.parseInt(ask("Server port", "5050"));

        buildWindow();
        connect(host, port);
    }

    private String ask(String title, String fallback) {
        String value = JOptionPane.showInputDialog(null, title, fallback);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private void buildWindow() {
        frame = new JFrame("RetroChat - " + username);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(760, 480));
        frame.setLayout(new BorderLayout(4, 4));
        frame.getContentPane().setBackground(NES_BLUE);

        transcript = new JTextPane();
        transcript.setEditable(false);
        transcript.setFont(PIXEL_FONT);
        transcript.setForeground(INK);
        transcript.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(transcript);
        scrollPane.setBorder(BorderFactory.createLineBorder(NES_CYAN, 4));

        userList = new JTextArea("ONLINE\n> " + username);
        userList.setEditable(false);
        userList.setFont(PIXEL_FONT);
        userList.setForeground(Color.WHITE);
        userList.setBackground(NES_BLUE.darker());
        userList.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(160, 0));
        rightPanel.setBackground(NES_BLUE);
        JLabel statusDot = new JLabel("  * ONLINE");
        statusDot.setFont(PIXEL_FONT);
        statusDot.setForeground(NES_GREEN);
        rightPanel.add(statusDot, BorderLayout.NORTH);
        rightPanel.add(userList, BorderLayout.CENTER);

        input = new PlaceholderTextField("Type your message here...");
        input.setFont(PIXEL_FONT);
        input.setForeground(INK);
        input.setBackground(Color.WHITE);
        input.setBorder(BorderFactory.createLineBorder(NES_CYAN, 3));
        input.addActionListener(this::sendMessage);

        sendButton = new JButton("SEND");
        sendButton.setFont(PIXEL_FONT);
        sendButton.setForeground(Color.WHITE);
        sendButton.setBackground(NES_PINK);
        sendButton.setFocusPainted(false);
        sendButton.addActionListener(this::sendMessage);

        JPanel bottomPanel = new JPanel(new BorderLayout(4, 0));
        bottomPanel.setBackground(NES_BLUE);
        bottomPanel.add(input, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        JPanel centerPanel = new JPanel(new GridLayout(1, 1));
        centerPanel.setBackground(NES_BLUE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 0));
        centerPanel.add(scrollPane);

        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(rightPanel, BorderLayout.EAST);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private void connect(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("JOIN|" + username);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(() -> readMessages(in), "server-listener").start();
        } catch (IOException error) {
            appendSystem("--:--", "Could not connect: " + error.getMessage());
            input.setEnabled(false);
            sendButton.setEnabled(false);
        }
    }

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

    private void handleServerLine(String line) {
        String[] parts = line.split("\\|", 4);
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

        appendSystem("--:--", line);
    }

    private void sendMessage(ActionEvent event) {
        String message = input.getText().trim();
        if (message.isEmpty() || out == null) {
            return;
        }

        out.println(message);
        input.setText("");

        if (message.equalsIgnoreCase("/quit")) {
            frame.dispose();
        }
    }

    private void appendMessage(String time, String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            appendSegment("> ", style(INK, false, false));
            appendSegment(time + " ", style(TIME_GRAY, true, false));
            appendSegment(sender + ": ", style(userColor(sender), false, true));
            appendSegment(message + System.lineSeparator(), style(INK, false, false));
            transcript.setCaretPosition(transcript.getDocument().getLength());
        });
    }

    private void appendSystem(String time, String message) {
        SwingUtilities.invokeLater(() -> {
            appendSegment("* ", style(INK, false, false));
            appendSegment(time + " ", style(TIME_GRAY, true, false));
            appendSegment("SERVER: ", style(SERVER_PURPLE, false, true));
            appendSegment(message + System.lineSeparator(), style(INK, false, false));
            transcript.setCaretPosition(transcript.getDocument().getLength());
        });
    }

    private SimpleAttributeSet style(Color color, boolean italic, boolean bold) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attributes, PIXEL_FONT.getFamily());
        StyleConstants.setFontSize(attributes, PIXEL_FONT.getSize());
        StyleConstants.setForeground(attributes, color);
        StyleConstants.setItalic(attributes, italic);
        StyleConstants.setBold(attributes, bold);
        return attributes;
    }

    private Color userColor(String sender) {
        int index = Math.abs(sender.toLowerCase().hashCode()) % USER_COLORS.length;
        return USER_COLORS[index];
    }

    private void updateUserList(String packedNames) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder list = new StringBuilder("ONLINE");
            if (!packedNames.isBlank()) {
                for (String name : packedNames.split(",")) {
                    if (!name.isBlank()) {
                        String marker = name.equalsIgnoreCase(username) ? "* " : "> ";
                        list.append(System.lineSeparator()).append(marker).append(name);
                    }
                }
            }
            userList.setText(list.toString());
        });
    }

    private void appendSegment(String text, SimpleAttributeSet attributes) {
        StyledDocument document = transcript.getStyledDocument();
        try {
            document.insertString(document.getLength(), text, attributes);
        } catch (BadLocationException error) {
            throw new IllegalStateException("Could not update chat transcript.", error);
        }
    }

    private static final class PlaceholderTextField extends JTextField {
        private final String placeholder;

        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!getText().isEmpty()) {
                return;
            }

            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            copy.setColor(PLACEHOLDER_GRAY);
            copy.setFont(getFont().deriveFont(Font.ITALIC));
            int x = getInsets().left + 4;
            int y = (getHeight() - copy.getFontMetrics().getHeight()) / 2 + copy.getFontMetrics().getAscent();
            copy.drawString(placeholder, x, y);
            copy.dispose();
        }
    }
}
