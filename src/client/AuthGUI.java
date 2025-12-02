
import javax.swing.*;
import java.awt.*;

public class AuthGUI {

    private static final Color WA_HEADER = new Color(7, 94, 84);
    private static final Color WA_ACCENT = new Color(37, 211, 102);
    private static final Color WA_PANEL = new Color(236, 229, 221);

    private Authentication authenticator;
    private JButton loginButton;
    private ConnectionManager connectionMgr;
    private JTextField username;
    private JPasswordField password;
    private JFrame frame;
    private ClientGUI clientGUI;

    public AuthGUI() {
        frame = new JFrame("Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        
        panel.add(usernameLabel);
        panel.add(username);
        panel.add(passwordLabel);
        panel.add(password);
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

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Please enter both username and password.",
                    "Login Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (authenticator == null) {
            JOptionPane.showMessageDialog(frame,
                    "Authentication service not initialized.",
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        User loggedInUser = authenticator.validateCredentials(user, pass);

        if (loggedInUser != null) {
            if (authenticator.checkStatus(loggedInUser)) {
                JOptionPane.showMessageDialog(frame,
                        "User is already logged in on another session.",
                        "Login Failed",
                        JOptionPane.WARNING_MESSAGE);
                password.setText("");
                return;
            }

            String sessionID = authenticator.createSession(loggedInUser);
            if (sessionID == null) {
                JOptionPane.showMessageDialog(frame,
                        "Failed to create session. Please try again.",
                        "Login Error",
                        JOptionPane.ERROR_MESSAGE);
                password.setText("");
                return;
            }

            frame.setVisible(false);
            frame.dispose();

            initClientGUI(loggedInUser);

        } else {
            JOptionPane.showMessageDialog(frame,
                    "Invalid username or password.",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
            password.setText("");
            username.requestFocus();
        }
    }

    public void initClientGUI(User u) {
        ClientGUI g1 = new ClientGUI(u);
        this.clientGUI = g1;
        g1.startManager();
    }

    // Setters for dependency injection
    public void setAuthenticator(Authentication authenticator) {
        this.authenticator = authenticator;
    }

    public void setConnectionMgr(ConnectionManager connectionMgr) {
        this.connectionMgr = connectionMgr;
    }
}
