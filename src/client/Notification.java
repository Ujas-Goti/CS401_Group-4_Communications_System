package client;

import javax.swing.BorderFactory;
import common.User;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.SystemTray;
import java.awt.Toolkit;

public class Notification {
    private final String messageID;
    private final User recipient;
    private boolean delivered;
    private String senderName;

    public Notification(String messageID, User recipient) {
        this.messageID = messageID;
        this.recipient = recipient;
        this.delivered = false;
        this.senderName = "Someone";
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName != null ? senderName : "Someone";
    }

    public void triggerNotification() {
        if (delivered) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog();
            dialog.setAlwaysOnTop(true);
            dialog.setUndecorated(true);
            dialog.setSize(300, 100);
            dialog.setLocationRelativeTo(null);

            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            dialog.setLocation(
                    (int) (screen.getWidth() - dialog.getWidth() - 20),
                    20
            );

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(new Color(37, 211, 102));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

            JLabel label = new JLabel("<html><b>New Message</b><br>" +
                    getSenderName() + " sent you a message</html>");
            label.setForeground(Color.WHITE);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            JButton close = new JButton("×");
            close.setFont(new Font("Arial", Font.BOLD, 16));
            close.setForeground(Color.WHITE);
            close.setContentAreaFilled(false);
            close.setBorderPainted(false);
            close.setFocusPainted(false);
            close.addActionListener(e -> {
                dialog.dispose();
                markAsDelivered();
            });

            panel.add(label, BorderLayout.CENTER);
            panel.add(close, BorderLayout.EAST);
            dialog.add(panel);
            dialog.setVisible(true);

            Timer timer = new Timer(5000, e -> {
                dialog.dispose();
                markAsDelivered();
            });
            timer.setRepeats(false);
            timer.start();
        });
    }

    public void markAsDelivered() {
        this.delivered = true;
    }

    public String getMessageID() {
        return messageID;
    }

    public User getRecipient() {
        return recipient;
    }

    public boolean isDelivered() {
        return delivered;
    }

    private String getSenderName() {
        return senderName;
    }

    public void triggerSystemTrayNotification() {
        if (SystemTray.isSupported() && !delivered) {
            triggerNotification(); // placeholder; tray icon can be added later
        }
    }
}
