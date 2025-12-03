package client;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import common.User;
import common.UserRole;

public class AuthGUI {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AuthGUI();
        });
    }

    private static final Color WA_HEADER = new Color(7, 94, 84);
    private static final Color WA_ACCENT = new Color(37, 211, 102);
    private static final Color WA_PANEL = new Color(236, 229, 221);

    private JButton loginButton;
    private JTextField username;
    private JPasswordField password;
    private JTextField serverHost;
    private JTextField serverPort;
    private JFrame frame;
    private ClientGUI clientGUI;
    private ClientConnection connection;

    public AuthGUI() {
        frame = new JFrame("Login");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(WA_PANEL);

        JPanel panel = new JPanel();
        panel.setBackground(WA_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        username = new JTextField(15);
        username.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        username.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 210, 207)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        password = new JPasswordField(15);
        password.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        password.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 210, 207)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setBackground(WA_ACCENT);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        usernameLabel.setForeground(new Color(74, 84, 82));

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        passwordLabel.setForeground(new Color(74, 84, 82));

        JLabel serverLabel = new JLabel("Server:");
        serverLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        serverLabel.setForeground(new Color(74, 84, 82));

        serverHost = new JTextField("localhost", 15);
        serverHost.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        serverHost.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 210, 207)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel portLabel = new JLabel("Port:");
        portLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        portLabel.setForeground(new Color(74, 84, 82));

        serverPort = new JTextField("1234", 15);
        serverPort.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        serverPort.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 210, 207)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        panel.setLayout(new java.awt.GridLayout(5, 2, 10, 10));
        panel.add(usernameLabel);
        panel.add(username);
        panel.add(passwordLabel);
        panel.add(password);
        panel.add(serverLabel);
        panel.add(serverHost);
        panel.add(portLabel);
        panel.add(serverPort);
        panel.add(new JLabel());
        panel.add(loginButton);

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onLoginClicked();
            }
        });

        password.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onLoginClicked();
                }
            }
        });
    }

    public void onLoginClicked() {
        String user = username.getText().trim();
        String pass = new String(password.getPassword());
        String host = serverHost.getText().trim();
        String portStr = serverPort.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Please enter both username and password.",
                    "Login Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (host.isEmpty() || portStr.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Please enter server host and port.",
                    "Login Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame,
                    "Invalid port number.",
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create connection
        connection = new ClientConnection(host, port);
        if (!connection.connect()) {
            JOptionPane.showMessageDialog(frame,
                    "Failed to connect to server. Please check server address and port.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Attempt login
        if (connection.login(user, pass)) {
            frame.setVisible(false);
            frame.dispose();

            // Get the actual User object from server (with correct role)
            User loggedInUser = connection.getLoggedInUser();
            if (loggedInUser == null) {
                // Fallback if server didn't send User
                loggedInUser = new User(user, pass, UserRole.GENERAL);
            }
            initClientGUI(loggedInUser, connection);
        } else {
            JOptionPane.showMessageDialog(frame,
                    "Invalid username or password, or user already logged in.",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
            password.setText("");
            connection.disconnect();
        }
    }

    public void initClientGUI(User u, ClientConnection conn) {
        ClientGUI g1 = new ClientGUI(u, conn);
        this.clientGUI = g1;
        g1.startManager();
    }
}
