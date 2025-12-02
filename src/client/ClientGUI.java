package client;

import javax.swing.*;
import javax.swing.BorderFactory;
import common.User;
import common.Message;
import common.UserRole;
import common.OnlineStatus;
import server.ChatSession;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientGUI {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color WA_HEADER = new Color(7, 94, 84);
    private static final Color WA_ACCENT = new Color(37, 211, 102);
    private static final Color WA_PANEL = new Color(236, 229, 221);

    private final User currentUser;
    private Notification notification;
    private ClientConnection connection;

    private final JFrame chat;
    private final JTabbedPane chatTabs;
    private final JList<User> usersList;
    private final JTextField searchBar;
    private final JLabel statusLabel;
    private final JLabel currentUserLabel;
    private final JButton createGroupButton;
    private final JButton viewLogsButton;
    private final JButton logoutButton;

    private final Map<String, JTextArea> chatAreas;
    private final Map<String, ChatSession> sessionsByChatId;
    private final Map<String, List<User>> groupMembers;
    private final Map<String, String> chatIdsByKey;
    private List<User> cachedUsers;
    private List<User> allRegisteredUsers; // All users from server (for group creation)

    public ClientGUI(User user, ClientConnection connection) {
        this.currentUser = user;
        this.connection = connection;
        this.notification = new Notification("", user);
        
        setupConnection();

        this.chat = new JFrame("Communication Client");
        this.chatTabs = new JTabbedPane();
        this.usersList = new JList<>();
        this.searchBar = new JTextField();
        this.statusLabel = new JLabel();
        this.currentUserLabel = new JLabel();
        this.createGroupButton = new JButton("New Group");
        this.viewLogsButton = new JButton("View Logs");
        this.logoutButton = new JButton("Logout");
        this.chatAreas = new HashMap<>();
        this.sessionsByChatId = new HashMap<>();
        this.groupMembers = new HashMap<>();
        this.cachedUsers = new ArrayList<>();
        this.allRegisteredUsers = new ArrayList<>();
        this.chatIdsByKey = new HashMap<>();

        initUI();
    }

    private void initUI() {
        chat.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chat.setLayout(new BorderLayout());
        chat.getContentPane().setBackground(WA_PANEL);
        chat.setPreferredSize(new Dimension(900, 600));

        chat.add(buildHeader(), BorderLayout.NORTH);
        chat.add(buildLeftPanel(), BorderLayout.WEST);
        chat.add(buildChatArea(), BorderLayout.CENTER);
        chat.add(buildStatusBar(), BorderLayout.SOUTH);

        bindActions();
        updateOwnStatus(currentUser != null ? currentUser.getStatus() : OnlineStatus.OFFLINE);
        displayAdminTools();
        chat.pack();
        chat.setLocationRelativeTo(null);
    }

    public void startManager() {
        chat.setVisible(true);
        
        // Request initial user list and all users
        if (connection != null && connection.isConnected()) {
            connection.requestUserList();
            connection.requestAllUsers(); // Get all users for group creation
        }
    }
    
    private void setupConnection() {
        if (connection == null) return;
        
        connection.setMessageListener(new ClientConnection.MessageListener() {
            @Override
            public void onMessageReceived(Message message) {
                SwingUtilities.invokeLater(() -> displayMessage(message));
            }
            
            @Override
            public void onUserListUpdated(List<User> users) {
                SwingUtilities.invokeLater(() -> updateOnlineUsers(users));
            }
            
            @Override
            public void onAllUsersReceived(List<User> users) {
                SwingUtilities.invokeLater(() -> updateAllUsers(users));
            }
            
            @Override
            public void onSessionReceived(ChatSession session, List<Message> history) {
                SwingUtilities.invokeLater(() -> {
                    String chatId = registerSession(session);
                    
                    // Add group to contact list if it's a group chat
                    if (session.isGroupChat()) {
                        String groupName = session.getChatName();
                        if (groupName != null && !groupName.isEmpty()) {
                            groupMembers.put(groupName, session.getParticipants());
                            addGroupContact(groupName, hasAnyOtherOnline(session.getParticipants()));
                            // Register with key for group
                            String key = "group:" + normalize(groupName);
                            chatIdsByKey.put(key, chatId);
                        }
                    } else {
                        // For private chats, register the key
                        if (session.getParticipants().size() == 2) {
                            User otherUser = null;
                            for (User p : session.getParticipants()) {
                                if (!p.getUserID().equals(currentUser.getUserID())) {
                                    otherUser = p;
                                    break;
                                }
                            }
                            if (otherUser != null) {
                                String key = "priv:" + otherUser.getUserID();
                                chatIdsByKey.put(key, chatId);
                            }
                        }
                    }
                    
                    // Only open window if not already open (to prevent duplicates)
                    boolean wasAlreadyOpen = isChatActive(chatId);
                    if (!wasAlreadyOpen) {
                        openChatWindow(session);
                    } else {
                        selectChatTab(chatId);
                    }
                    
                    // Only display history messages if window was just opened (not if it was already open)
                    if (!wasAlreadyOpen && history != null) {
                        for (Message msg : history) {
                            displayMessage(msg);
                        }
                    }
                });
            }
            
            @Override
            public void onNewSessionNotification(String chatID) {
                // Request session details from server
                SwingUtilities.invokeLater(() -> {
                    // The session should be sent by server, but if not, we can request it
                    // For now, the server sends the session when user opens it
                });
            }
        });
        
        // Start the listener thread now that message listener is set
        if (connection.isConnected()) {
            connection.startListenerThread();
        }
    }


    public void displayUserList(List<User> users) {
        cachedUsers = new ArrayList<>();
        DefaultListModel<User> model = new DefaultListModel<>();
        if (users != null) {
            for (User user : users) {
                if (user == null) {
                    continue;
                }
                if (currentUser != null && user.getUserID().equals(currentUser.getUserID())) {
                    continue;
                }
                cachedUsers.add(user);
                model.addElement(user);
            }
        }
        usersList.setModel(model);
    }

    public void updateOnlineUsers(List<User> users) {
        displayUserList(users);
    }
    
    public void updateAllUsers(List<User> users) {
        // Store all registered users (for group creation)
        allRegisteredUsers = new ArrayList<>();
        if (users != null) {
            for (User user : users) {
                if (user == null) {
                    continue;
                }
                // Don't include current user
                if (currentUser != null && user.getUserID().equals(currentUser.getUserID())) {
                    continue;
                }
                allRegisteredUsers.add(user);
            }
        }
    }

    public void filterUserList(String query) {
        if (cachedUsers == null) {
            return;
        }
        String term = query == null ? "" : query.trim().toLowerCase();
        DefaultListModel<User> model = new DefaultListModel<>();
        for (User user : cachedUsers) {
            if (term.isEmpty() || user.getUsername().toLowerCase().contains(term)) {
                model.addElement(user);
            }
        }
        usersList.setModel(model);
    }

    public void startPrivateChat(User selectedUser) {
        if (selectedUser == null) {
            return;
        }
        String key = "priv:" + selectedUser.getUserID();
        String chatId = chatIdsByKey.get(key);
        if (chatId != null && isChatActive(chatId)) {
            selectChatTab(chatId);
            return;
        }

        List<User> participants = new ArrayList<>();
        if (currentUser != null) {
            participants.add(currentUser);
        }
        participants.add(selectedUser);

        // Request session creation from server
        if (connection != null && connection.isConnected()) {
            connection.createSession(participants, false, selectedUser.getUsername());
        } else {
            // Fallback: create local session
            ChatSession session = new ChatSession(participants, false, selectedUser.getUsername());
            registerSessionWithKey(session, key);
            openChatWindow(session);
        }
    }

    public void startGroupChat(List<User> selectedUsers, String name) {
        if (selectedUsers == null || selectedUsers.isEmpty()) {
            return;
        }
        if (name == null || name.isEmpty()) {
            return;
        }
        String key = "group:" + normalize(name);
        String chatId = chatIdsByKey.get(key);
        if (chatId != null && isChatActive(chatId)) {
            selectChatTab(chatId);
            return;
        }
        List<User> members = new ArrayList<>(selectedUsers);
        if (currentUser != null && !members.contains(currentUser)) {
            members.add(0, currentUser);
        }
        
        // Request session creation from server
        if (connection != null && connection.isConnected()) {
            connection.createSession(members, true, name);
        } else {
            // Fallback: create local session
            ChatSession session = new ChatSession(members, true, name);
            groupMembers.put(name, members);
            addGroupContact(name, hasAnyOtherOnline(members));
            registerSessionWithKey(session, key);
            openChatWindow(session);
        }
    }

    public void createGroupChatDialog() {
        // Request all users from server if we don't have them yet
        if (connection != null && connection.isConnected()) {
            if (allRegisteredUsers == null || allRegisteredUsers.isEmpty()) {
                connection.requestAllUsers();
                // The response will update allRegisteredUsers via updateAllUsers callback
                // For now, show a message
                JOptionPane.showMessageDialog(chat, 
                    "Loading users from server... Please click 'New Group' again in a moment.", 
                    "Loading Users", 
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        } else {
            JOptionPane.showMessageDialog(chat, "No connection to server");
            return;
        }
        
        // Use all registered users for group creation (not just online ones)
        if (allRegisteredUsers == null || allRegisteredUsers.isEmpty()) {
            JOptionPane.showMessageDialog(chat, "No users available");
            return;
        }
        JDialog dialog = new JDialog(chat, "Create Group", true);
        dialog.setLayout(new BorderLayout());
        
        // Create a list model with all registered users
        DefaultListModel<User> allUsersModel = new DefaultListModel<>();
        for (User user : allRegisteredUsers) {
            allUsersModel.addElement(user);
        }
        
        JList<User> selectionList = new JList<>(allUsersModel);
        selectionList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectionList.setCellRenderer(new ContactRenderer());
        JTextField nameField = new JTextField();
        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> {
            List<User> chosen = selectionList.getSelectedValuesList();
            String groupName = nameField.getText().trim();
            if (!chosen.isEmpty() && !groupName.isEmpty()) {
                startGroupChat(chosen, groupName);
                dialog.dispose();
            }
        });

        dialog.add(nameField, BorderLayout.NORTH);
        dialog.add(new JScrollPane(selectionList), BorderLayout.CENTER);
        dialog.add(createButton, BorderLayout.SOUTH);
        dialog.setSize(300, 400);
        dialog.setLocationRelativeTo(chat);
        dialog.setVisible(true);
    }

    public void openChatWindow(ChatSession session) {
        if (session == null) {
            return;
        }
        String chatId = registerSession(session);
        
        // Check if chat window already exists - if so, just select it
        if (chatAreas.containsKey(chatId)) {
            selectChatTab(chatId);
            return;
        }
        
        // Check if tab already exists by checking all tabs
        for (int i = 0; i < chatTabs.getTabCount(); i++) {
            Component comp = chatTabs.getComponentAt(i);
            if (comp instanceof JPanel) {
                // Check if this panel contains a chat area for this session
                JTextArea existingArea = findTextAreaInPanel((JPanel) comp);
                if (existingArea != null) {
                    // Find which chatId this area belongs to
                    for (Map.Entry<String, JTextArea> entry : chatAreas.entrySet()) {
                        if (entry.getValue() == existingArea) {
                            if (entry.getKey().equals(chatId)) {
                                selectChatTab(chatId);
                                return;
                            }
                        }
                    }
                }
            }
        }

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setBackground(new Color(250, 246, 241));
        area.setMargin(new Insets(12, 12, 12, 12));

        JTextField input = new JTextField();
        JButton send = new JButton("Send");
        send.setBackground(WA_ACCENT);
        send.setForeground(Color.WHITE);
        send.setFocusPainted(false);
        send.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setBackground(WA_HEADER);
        chatHeader.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        JLabel title = new JLabel(session.isGroupChat() ? "Group: " + session.getChatName() : session.getChatName());
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        chatHeader.add(title, BorderLayout.WEST);

        if (session.isGroupChat()) {
            JButton membersButton = new JButton("Members");
            membersButton.setForeground(Color.WHITE);
            membersButton.setBackground(WA_HEADER);
            membersButton.setFocusPainted(false);
            membersButton.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            membersButton.addActionListener(e -> showGroupMembersDialog(session.getChatName()));
            chatHeader.add(membersButton, BorderLayout.EAST);
        }

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(chatHeader, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));
        inputPanel.add(input, BorderLayout.CENTER);
        inputPanel.add(send, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);

        send.addActionListener(e -> {
            String content = input.getText().trim();
            if (!content.isEmpty()) {
                sendMessage(session, content);
                input.setText("");
            }
        });

        chatAreas.put(chatId, area);
        addChatTab(chatId, panel);
    }

    public void displayMessage(Message message) {
        if (message == null) {
            return;
        }
        String chatId = message.getChatID();
        JTextArea area = chatAreas.get(chatId);
        if (area == null) {
            ChatSession session = sessionsByChatId.get(chatId);
            if (session != null) {
                openChatWindow(session);
                area = chatAreas.get(chatId);
            }
        }
        if (area == null) {
            return;
        }
        String sender = message.getSenderID();
        LocalDateTime stamp = message.getTimeStamp() != null ? message.getTimeStamp() : LocalDateTime.now();
        String line = String.format("[%s] %s: %s", stamp.format(TIME_FMT), sender, message.getContent());
        area.append(line + System.lineSeparator());
        area.setCaretPosition(area.getDocument().getLength());

        if (!isChatActive(chatId) && notification != null) {
            notification.setSenderName(sender);
            notification.triggerNotification();
        }
    }

    public void sendMessage(ChatSession session, String content) {
        if (session == null || content == null || content.isEmpty()) {
            return;
        }
        String chatId = registerSession(session);
        String senderId = currentUser != null && currentUser.getUserID() != null
                ? currentUser.getUserID()
                : currentUser != null ? currentUser.getUsername() : "unknown";
        Message message = new Message(chatId, senderId, content);
        
        // Send via network
        if (connection != null && connection.isConnected()) {
            connection.sendMessage(message);
        } else {
            // Fallback: just display locally if no connection
            displayMessage(message);
        }
    }

    public void updateOwnStatus(OnlineStatus status) {
        if (currentUser != null && status != null) {
            currentUser.setStatus(status);
        }
        statusLabel.setText("Status: " + (status != null ? status.name() : "UNKNOWN"));
    }

    public boolean isChatActive(String chatId) {
        return chatAreas.containsKey(chatId);
    }

    public void displayAdminTools() {
        boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;
        viewLogsButton.setVisible(isAdmin);
    }

    public void viewChatLogsDialog() {
        if (connection == null || !connection.isConnected()) {
            JOptionPane.showMessageDialog(chat, "Not connected to server.");
            return;
        }
        
        // Create a dialog to view chat logs
        JDialog logDialog = new JDialog(chat, "Chat Logs", true);
        logDialog.setSize(800, 600);
        logDialog.setLocationRelativeTo(chat);
        
        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        
        // Read chat log file
        try {
            java.io.File logFile = new java.io.File("data/chat_log.txt");
            if (logFile.exists()) {
                java.util.Scanner scanner = new java.util.Scanner(logFile);
                StringBuilder content = new StringBuilder();
                while (scanner.hasNextLine()) {
                    content.append(scanner.nextLine()).append("\n");
                }
                scanner.close();
                logArea.setText(content.toString());
            } else {
                logArea.setText("No log file found.");
            }
        } catch (Exception e) {
            logArea.setText("Error reading log file: " + e.getMessage());
        }
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> logDialog.dispose());
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Chat Log Contents:"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(closeButton, BorderLayout.SOUTH);
        
        logDialog.add(panel);
        logDialog.setVisible(true);
    }

    public void logout(User user) {
        if (connection != null && connection.isConnected()) {
            connection.logout();
        }
        chat.dispose();
    }

    // === helper methods ===

    private void addChatTab(String chatId, JPanel panel) {
        for (int i = 0; i < chatTabs.getTabCount(); i++) {
            if (chatTabs.getComponentAt(i) == panel) {
                chatTabs.setSelectedIndex(i);
                return;
            }
        }
        chatTabs.addTab(sessionTitle(chatId), panel);
        chatTabs.setSelectedComponent(panel);
    }

    private String sessionTitle(String chatId) {
        ChatSession session = sessionsByChatId.get(chatId);
        if (session != null) {
            return session.getChatName();
        }
        return displayNameForChat(chatId);
    }

    private String displayNameForChat(String chatId) {
        if (chatId == null || chatId.isEmpty()) {
            return "";
        }
        ChatSession session = sessionsByChatId.get(chatId);
        if (session != null && session.getChatName() != null && !session.getChatName().isEmpty()) {
            return session.getChatName();
        }
        return chatId;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(WA_HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        currentUserLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        currentUserLabel.setForeground(Color.WHITE);
        String label = currentUser == null
                ? "Unknown"
                : currentUser.getUsername() + " • " + currentUser.getRole();
        currentUserLabel.setText(label);
        header.add(currentUserLabel, BorderLayout.WEST);
        return header;
    }

    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setPreferredSize(new Dimension(280, 0));
        left.setBackground(WA_PANEL);
        left.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(210, 205, 195)));

        JPanel searchPanel = new JPanel(new BorderLayout(4, 4));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));
        JLabel searchLabel = new JLabel("Search or start a new chat");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchLabel.setForeground(new Color(110, 128, 115));
        searchPanel.add(searchLabel, BorderLayout.NORTH);
        searchPanel.add(searchBar, BorderLayout.CENTER);

        usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usersList.setCellRenderer(new ContactRenderer());
        JScrollPane listScroll = new JScrollPane(usersList);
        listScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);
        styleSecondaryButton(createGroupButton);
        styleSecondaryButton(viewLogsButton);
        styleSecondaryButton(logoutButton);
        buttonPanel.add(createGroupButton, gbc);
        gbc.gridy = 1;
        buttonPanel.add(viewLogsButton, gbc);
        gbc.gridy = 2;
        buttonPanel.add(logoutButton, gbc);

        left.add(searchPanel, BorderLayout.NORTH);
        left.add(listScroll, BorderLayout.CENTER);
        left.add(buttonPanel, BorderLayout.SOUTH);
        return left;
    }

    private JPanel buildChatArea() {
        chatTabs.setFont(new Font("Segoe UI", Font.BOLD, 15));
        chatTabs.setBackground(Color.WHITE);
        chatTabs.setBorder(BorderFactory.createEmptyBorder());
        chatTabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tabPlacement, int runCount, int maxTabHeight) {
                return 0;
            }
        });
        return new JPanel(new BorderLayout()) {
            {
                add(chatTabs, BorderLayout.CENTER);
            }
        };
    }

    private JPanel buildStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(WA_PANEL);
        statusBar.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(74, 84, 82));
        statusBar.add(statusLabel, BorderLayout.WEST);
        return statusBar;
    }

    private void bindActions() {
        searchBar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterUserList(searchBar.getText()); }
            @Override public void removeUpdate(DocumentEvent e) { filterUserList(searchBar.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { filterUserList(searchBar.getText()); }
        });

        usersList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    User selected = usersList.getSelectedValue();
                    if (selected != null) {
                        openChatForContact(selected);
                    }
                }
            }
        });

        createGroupButton.addActionListener(this::handleCreateGroup);
        viewLogsButton.addActionListener(e -> viewChatLogsDialog());
        logoutButton.addActionListener(e -> logout(currentUser));
    }

    private void handleCreateGroup(ActionEvent e) {
        createGroupChatDialog();
    }

    private void styleSecondaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setBackground(WA_ACCENT);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 120, 90)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    private boolean addGroupContact(String groupName, boolean anyOnline) {
        if (groupName == null || groupName.isEmpty()) {
            return false;
        }
        for (User user : cachedUsers) {
            if (groupName.equals(user.getUsername())) {
                user.setStatus(anyOnline ? OnlineStatus.ONLINE : OnlineStatus.OFFLINE);
                usersList.repaint();
                return false;
            }
        }
        User groupUser = new User(groupName, "", UserRole.GENERAL);
        groupUser.setStatus(anyOnline ? OnlineStatus.ONLINE : OnlineStatus.OFFLINE);
        cachedUsers.add(groupUser);
        if (usersList.getModel() instanceof DefaultListModel<User> model) {
            model.addElement(groupUser);
        }
        return true;
    }

    private void showGroupMembersDialog(String groupName) {
        List<User> members = groupMembers.get(groupName);
        if (members == null || members.isEmpty()) {
            JOptionPane.showMessageDialog(chat, "No members recorded for this group.");
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Members of ").append(groupName).append(":\n\n");
        User admin = members.get(0);
        for (User member : members) {
            builder.append("- ").append(member.getUsername());
            if (member.equals(admin)) {
                builder.append(" (admin)");
            }
            builder.append('\n');
        }
        JOptionPane.showMessageDialog(chat, builder.toString(), "Group Members", JOptionPane.INFORMATION_MESSAGE);
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().replaceAll("\\s+", "_").toLowerCase();
    }

    private boolean hasAnyOtherOnline(List<User> members) {
        if (members == null) {
            return false;
        }
        for (User member : members) {
            if (member == null || currentUser == null) {
                continue;
            }
            if (!member.getUserID().equals(currentUser.getUserID()) && member.getStatus() == OnlineStatus.ONLINE) {
                return true;
            }
        }
        return false;
    }

    private String registerSession(ChatSession session) {
        if (session == null) {
            return "";
        }
        sessionsByChatId.putIfAbsent(session.getChatID(), session);
        return session.getChatID();
    }

    private void registerSessionWithKey(ChatSession session, String key) {
        if (session == null) {
            return;
        }
        String chatId = registerSession(session);
        if (key != null && !key.isEmpty()) {
            chatIdsByKey.put(key, chatId);
        }
    }

    private void openChatForContact(User contact) {
        if (contact == null) {
            return;
        }
        String name = contact.getUsername();
        boolean isGroupContact = groupMembers.containsKey(name);
        String key = isGroupContact ? "group:" + normalize(name) : "priv:" + contact.getUserID();
        String chatId = chatIdsByKey.get(key);
        
        // Check if chat is already open
        if (chatId != null && isChatActive(chatId)) {
            selectChatTab(chatId);
            return;
        }
        
        // Check if session exists but window not open
        if (chatId != null) {
            ChatSession session = sessionsByChatId.get(chatId);
            if (session != null) {
                openChatWindow(session);
                return;
            }
        }
        
        // If no existing session, start a new chat
        if (isGroupContact) {
            // For group chats, request session from server
            List<User> members = groupMembers.get(name);
            if (members != null && connection != null && connection.isConnected()) {
                connection.createSession(members, true, name);
            }
        } else {
            // For private chats, request session from server
            startPrivateChat(contact);
        }
    }

    private void selectChatTab(String chatId) {
        JTextArea area = chatAreas.get(chatId);
        if (area == null) {
            return;
        }
        for (int i = 0; i < chatTabs.getTabCount(); i++) {
            Component comp = chatTabs.getComponentAt(i);
            if (comp instanceof JPanel panel && panel.isAncestorOf(area)) {
                chatTabs.setSelectedIndex(i);
                return;
            }
        }
    }
    
    private JTextArea findTextAreaInPanel(JPanel panel) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JTextArea) {
                return (JTextArea) comp;
            } else if (comp instanceof JScrollPane) {
                JScrollPane scroll = (JScrollPane) comp;
                Component view = scroll.getViewport().getView();
                if (view instanceof JTextArea) {
                    return (JTextArea) view;
                }
            } else if (comp instanceof JPanel) {
                JTextArea found = findTextAreaInPanel((JPanel) comp);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static class ContactRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof User user) {
                label.setText(user.getUsername());
                label.setOpaque(true);
                label.setBackground(isSelected ? new Color(202, 231, 217) : new Color(236, 229, 221));
                label.setForeground(user.getStatus() == OnlineStatus.ONLINE ? new Color(37, 131, 59) : Color.BLACK);
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(207, 210, 207)),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)
                ));
            }
            return label;
        }
    }
}