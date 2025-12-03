package client;

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

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
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
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

//Everything must be imported one by one o9therwise it wouldnt work (Ujas)

import common.Message;
import common.OnlineStatus;
import common.User;
import common.UserRole;
import server.ChatSession;

public class ClientGUI {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color WA_HEADER = new Color(7, 94, 84);
    private static final Color WA_ACCENT = new Color(37, 211, 102);
    private static final Color WA_PANEL = new Color(236, 229, 221);

    private final User user;
    private Notification notif;
    private ClientConnection conn;

    private final JFrame frame;
    private final JTabbedPane tabs;
    private final JList<User> userList;
    private final JTextField search;
    private final JLabel status;
    private final JLabel userLabel;
    private final JButton groupBtn;
    private final JButton logsBtn;
    private final JButton logoutBtn;

    private final Map<String, JTextArea> areas;
    private final Map<String, ChatSession> sessions;
    private final Map<String, List<User>> groups;
    private final Map<String, String> keys;
    private List<User> cached;
    private List<User> allUsers; //All users from server (for group creation)

    public ClientGUI(User user, ClientConnection conn) {
        this.user = user;
        this.conn = conn;
        this.notif = new Notification("", user);

        setupConn();

        this.frame = new JFrame("Communication Client");
        this.tabs = new JTabbedPane();
        this.userList = new JList<>();
        this.search = new JTextField();
        this.status = new JLabel();
        this.userLabel = new JLabel();
        this.groupBtn = new JButton("New Group");
        this.logsBtn = new JButton("View Logs");
        this.logoutBtn = new JButton("Logout");
        this.areas = new HashMap<>();
        this.sessions = new HashMap<>();
        this.groups = new HashMap<>();
        this.cached = new ArrayList<>();
        this.allUsers = new ArrayList<>();
        this.keys = new HashMap<>();

        initUI();
    }

    private void initUI() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(WA_PANEL);
        frame.setPreferredSize(new Dimension(900, 600));

        frame.add(buildHeader(), BorderLayout.NORTH);
        frame.add(buildLeftPanel(), BorderLayout.WEST);
        frame.add(buildChatArea(), BorderLayout.CENTER);
        frame.add(buildStatusBar(), BorderLayout.SOUTH);

        bindActions();
        updateStatus(user != null ? user.getStatus() : OnlineStatus.OFFLINE);
        showAdminTools();
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public void start() {
        frame.setVisible(true);

        //Request initial user list and all users
        if (conn != null && conn.isConnected()) {
            conn.requestUsers();
            conn.requestAll(); //Get all users for group creation and Carlos M try to fix the error
        }
    }

    private void setupConn() {
        if (conn == null) {
			return;
		}

        conn.setListener(new ClientConnection.MessageListener() {
            @Override
            public void onMessageReceived(Message message) {
                SwingUtilities.invokeLater(() -> showMessage(message));
            }

            @Override
            public void onUserListUpdated(List<User> users) {
                SwingUtilities.invokeLater(() -> refreshUsers(users));
            }

            @Override
            public void onAllUsersReceived(List<User> users) {
                SwingUtilities.invokeLater(() -> updateAll(users));
            }

            @Override
            public void onSessionReceived(ChatSession session, List<Message> history) {
                SwingUtilities.invokeLater(() -> {
                    String id = register(session);

                    //Add group to contact list if it's a group frame
                    if (session.isGroupChat()) {
                        String groupName = session.getChatName();
                        if (groupName != null && !groupName.isEmpty()) {
                            groups.put(groupName, session.getParticipants());
                            addGroup(groupName, hasOtherOnline(session.getParticipants()));
                            //Register with key for group
                            String key = "group:" + normalize(groupName);
                            keys.put(key, id);
                        }
                    } else {
                        //For private frames, register the key
                        if (session.getParticipants().size() == 2) {
                            User otherUser = null;
                            for (User p : session.getParticipants()) {
                                if (!p.getUserID().equals(user.getUserID())) {
                                    otherUser = p;
                                    break;
                                }
                            }
                            if (otherUser != null) {
                                String key = "priv:" + otherUser.getUserID();
                                keys.put(key, id);
                            }
                        }
                    }

                    //Only open window if not already open (to prevent duplicates)
                    boolean wasAlreadyOpen = isOpen(id);
                    if (!wasAlreadyOpen) {
                        openChat(session);
                    } else {
                        selectTab(id);
                    }

                    //Only display history messages if window was just opened (not if it was already open)
                    if (!wasAlreadyOpen && history != null) {
                        for (Message msg : history) {
                            showMessage(msg);
                        }
                    }
                });
            }

            @Override
            public void onNewSessionNotification(String frameID) {
                //Request session details from server
                SwingUtilities.invokeLater(() -> {
                    //The session should be sent by server, but if not, we can request it
                    //For now, the server sends the session when user opens it
                });
            }

            @Override
            public void onChatLogsReceived(String logContent) {
                SwingUtilities.invokeLater(() -> {
                    //Create dialog to display logs
                    JDialog logDialog = new JDialog(frame, "Chat Logs", true);
                    logDialog.setSize(800, 600);
                    logDialog.setLocationRelativeTo(frame);

                    JTextArea logArea = new JTextArea();
                    logArea.setEditable(false);
                    logArea.setFont(new Font("Courier New", Font.PLAIN, 12));
                    logArea.setText(logContent);
                    JScrollPane scrollPane = new JScrollPane(logArea);

                    JButton closeButton = new JButton("Close");
                    closeButton.addActionListener(e -> logDialog.dispose());

                    JPanel panel = new JPanel(new BorderLayout());
                    panel.add(new JLabel("Chat Log Contents:"), BorderLayout.NORTH);
                    panel.add(scrollPane, BorderLayout.CENTER);
                    panel.add(closeButton, BorderLayout.SOUTH);

                    logDialog.add(panel);
                    logDialog.setVisible(true);
                });
            }
        });

        //Start the listener thread now that message listener is set
        if (conn.isConnected()) {
            conn.startListener();
        }
    }


    public void showUsers(List<User> users) {
        cached = new ArrayList<>();
        DefaultListModel<User> model = new DefaultListModel<>();
        if (users != null) {
            for (User u : users) {
                if ((u == null) || (user != null && u.getUserID().equals(user.getUserID()))) {
                    continue;
                }
                cached.add(u);
                model.addElement(u);
            }
        }
        userList.setModel(model);
    }

    public void refreshUsers(List<User> users) {
        showUsers(users);
    }

    public void updateAll(List<User> users) {
        //Store all registered users (for group creation)
        allUsers = new ArrayList<>();
        if (users != null) {
            for (User u : users) {
                //Don't include current user
                if ((u == null) || (user != null && u.getUserID().equals(user.getUserID()))) {
                    continue;
                }
                allUsers.add(u);
            }
        }
    }

    public void filterUsers(String query) {
        if (cached == null) {
            return;
        }
        String term = query == null ? "" : query.trim().toLowerCase();
        DefaultListModel<User> model = new DefaultListModel<>();
        for (User u : cached) {
            if (term.isEmpty() || u.getUsername().toLowerCase().contains(term)) {
                model.addElement(u);
            }
        }
        userList.setModel(model);
    }

    public void startPrivate(User selectedUser) {
        if (selectedUser == null) {
            return;
        }
        String key = "priv:" + selectedUser.getUserID();
        String id = keys.get(key);
        if (id != null && isOpen(id)) {
            selectTab(id);
            return;
        }

        List<User> participants = new ArrayList<>();
        if (user != null) {
            participants.add(user);
        }
        participants.add(selectedUser);

        //Request session creation from server
        if (conn != null && conn.isConnected()) {
            conn.create(participants, false, selectedUser.getUsername());
        } else {
            //Fallback: create local session
            ChatSession session = new ChatSession(participants, false, selectedUser.getUsername());
            registerKey(session, key);
            openChat(session);
        }
    }

    public void startGroup(List<User> selectedUsers, String name) {
        if (selectedUsers == null || selectedUsers.isEmpty() || name == null || name.isEmpty()) {
            return;
        }
        String key = "group:" + normalize(name);
        String id = keys.get(key);
        if (id != null && isOpen(id)) {
            selectTab(id);
            return;
        }
        List<User> members = new ArrayList<>(selectedUsers);
        if (user != null && !members.contains(user)) {
            members.add(0, user);
        }

        //Request session creation from server
        if (conn != null && conn.isConnected()) {
            conn.create(members, true, name);
        } else {
            //Fallback: create local session
            ChatSession session = new ChatSession(members, true, name);
            groups.put(name, members);
            addGroup(name, hasOtherOnline(members));
            registerKey(session, key);
            openChat(session);
        }
    }

    public void showGroupDialog() {
        //Request all users from server if we don't have them yet
        if (conn != null && conn.isConnected()) {
            if (allUsers == null || allUsers.isEmpty()) {
                conn.requestAll();
                //The response will update allUsers via updateAll callback
                //For now, show a message
                JOptionPane.showMessageDialog(frame,
                    "Loading users from server... Please click 'New Group' again in a moment.",
                    "Loading Users",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        } else {
            JOptionPane.showMessageDialog(frame, "No conn to server");
            return;
        }

        //Use all registered users for group creation (not just online ones)
        if (allUsers == null || allUsers.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No users available");
            return;
        }
        JDialog dialog = new JDialog(frame, "Create Group", true);
        dialog.setLayout(new BorderLayout());

        //Create a list model with all registered users
        DefaultListModel<User> allUsersModel = new DefaultListModel<>();
        for (User user : allUsers) {
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
                startGroup(chosen, groupName);
                dialog.dispose();
            }
        });

        dialog.add(nameField, BorderLayout.NORTH);
        dialog.add(new JScrollPane(selectionList), BorderLayout.CENTER);
        dialog.add(createButton, BorderLayout.SOUTH);
        dialog.setSize(300, 400);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    public void openChat(ChatSession session) {
        if (session == null) {
            return;
        }
        String id = register(session);

        //Check if frame window already exists - if so, just select it
        if (areas.containsKey(id)) {
            selectTab(id);
            return;
        }

        //Check if tab already exists by checking all tabs
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component comp = tabs.getComponentAt(i);
            if (comp instanceof JPanel) {
                //Check if this panel contains a frame area for this session
                JTextArea existingArea = findTextAreaInPanel((JPanel) comp);
                if (existingArea != null) {
                    //Find which id this area belongs to
                    for (Map.Entry<String, JTextArea> entry : areas.entrySet()) {
                        if (entry.getValue() == existingArea) {
                            if (entry.getKey().equals(id)) {
                                selectTab(id);
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

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(WA_HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        
        //Determine the display name: for private chats, show the other participant's name
        String displayName;
        if (session.isGroupChat()) {
            displayName = "Group: " + session.getChatName();
        } else {
            //For private chats, find the other participant (not current user)
            displayName = session.getChatName();
            List<User> participants = session.getParticipants();
            if (participants != null && participants.size() == 2 && user != null) {
                for (User participant : participants) {
                    if (!participant.getUserID().equals(user.getUserID())) {
                        displayName = participant.getUsername();
                        break;
                    }
                }
            }
        }
        
        JLabel title = new JLabel(displayName);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.add(title, BorderLayout.WEST);

        if (session.isGroupChat()) {
            JButton membersButton = new JButton("Members");
            membersButton.setForeground(Color.WHITE);
            membersButton.setBackground(WA_HEADER);
            membersButton.setFocusPainted(false);
            membersButton.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            membersButton.addActionListener(e -> showMembers(session.getChatName()));
            header.add(membersButton, BorderLayout.EAST);
        }

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(header, BorderLayout.NORTH);
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

        areas.put(id, area);
        addChatTab(id, panel);
    }

    public void showMessage(Message message) {
        if (message == null) {
            return;
        }
        String id = message.getChatID();
        JTextArea area = areas.get(id);
        if (area == null) {
            ChatSession session = sessions.get(id);
            if (session != null) {
                openChat(session);
                area = areas.get(id);
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

        if (!isOpen(id) && notif != null) {
            notif.setSenderName(sender);
            notif.triggerNotification();
        }
    }

    public void sendMessage(ChatSession session, String content) {
        if (session == null || content == null || content.isEmpty()) {
            return;
        }
        String id = register(session);
        String senderId = user != null && user.getUserID() != null
                ? user.getUserID()
                : user != null ? user.getUsername() : "unknown";
        Message message = new Message(id, senderId, content);

        //Send via network
        if (conn != null && conn.isConnected()) {
            conn.send(message);
        } else {
            //Fallback: just display locally if no conn
            showMessage(message);
        }
    }

    public void updateStatus(OnlineStatus st) {
        if (user != null && st != null) {
            user.setStatus(st);
        }
        status.setText("Status: " + (st != null ? st.name() : "UNKNOWN"));
    }

    public boolean isOpen(String id) {
        return areas.containsKey(id);
    }

    public void showAdminTools() {
        boolean isAdmin = user != null && user.getRole() == UserRole.ADMIN;
        logsBtn.setVisible(isAdmin);
    }

    public void showLogs() {
        if (conn == null || !conn.isConnected()) {
            JOptionPane.showMessageDialog(frame, "Not connected to server.");
            return;
        }

        //Create a dialog to view frame logs
        JDialog logDialog = new JDialog(frame, "Chat Logs", true);
        logDialog.setSize(800, 600);
        logDialog.setLocationRelativeTo(frame);

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        logArea.setText("Loading logs from server...");
        JScrollPane scrollPane = new JScrollPane(logArea);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> logDialog.dispose());

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Chat Log Contents:"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(closeButton, BorderLayout.SOUTH);

        logDialog.add(panel);
        logDialog.setVisible(true);

        //Request logs from server
        conn.requestLogs();
    }

    public void logout(User user) {
        if (conn != null && conn.isConnected()) {
            conn.logout();
        }
        frame.dispose();
    }

    //=== helper methods ===

    private void addChatTab(String id, JPanel panel) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (tabs.getComponentAt(i) == panel) {
                tabs.setSelectedIndex(i);
                return;
            }
        }
        tabs.addTab(getTitle(id), panel);
        tabs.setSelectedComponent(panel);
    }

    private String getTitle(String id) {
        ChatSession session = sessions.get(id);
        if (session != null) {
            return session.getChatName();
        }
        return getName(id);
    }

    private String getName(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        ChatSession session = sessions.get(id);
        if (session != null && session.getChatName() != null && !session.getChatName().isEmpty()) {
            return session.getChatName();
        }
        return id;
    }

    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(WA_HEADER);
        hdr.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        userLabel.setForeground(Color.WHITE);
        String lbl = user == null
                ? "Unknown"
                : user.getUsername() + " • " + user.getRole();
        userLabel.setText(lbl);
        hdr.add(userLabel, BorderLayout.WEST);
        return hdr;
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
        searchPanel.add(search, BorderLayout.CENTER);

        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setCellRenderer(new ContactRenderer());
        JScrollPane listScroll = new JScrollPane(userList);
        listScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);
        styleSecondaryButton(groupBtn);
        styleSecondaryButton(logsBtn);
        styleSecondaryButton(logoutBtn);
        buttonPanel.add(groupBtn, gbc);
        gbc.gridy = 1;
        buttonPanel.add(logsBtn, gbc);
        gbc.gridy = 2;
        buttonPanel.add(logoutBtn, gbc);

        left.add(searchPanel, BorderLayout.NORTH);
        left.add(listScroll, BorderLayout.CENTER);
        left.add(buttonPanel, BorderLayout.SOUTH);
        return left;
    }

    private JPanel buildChatArea() {
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tabs.setBackground(Color.WHITE);
        tabs.setBorder(BorderFactory.createEmptyBorder());
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tabPlacement, int runCount, int maxTabHeight) {
                return 0;
            }
        });
        return new JPanel(new BorderLayout()) {
            {
                add(tabs, BorderLayout.CENTER);
            }
        };
    }

    private JPanel buildStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(WA_PANEL);
        statusBar.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
        status.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        status.setForeground(new Color(74, 84, 82));
        statusBar.add(status, BorderLayout.WEST);
        return statusBar;
    }

    private void bindActions() {
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterUsers(search.getText()); }
            @Override public void removeUpdate(DocumentEvent e) { filterUsers(search.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { filterUsers(search.getText()); }
        });

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    User selected = userList.getSelectedValue();
                    if (selected != null) {
                        openFor(selected);
                    }
                }
            }
        });

        groupBtn.addActionListener(this::handleCreateGroup);
        logsBtn.addActionListener(e -> showLogs());
        logoutBtn.addActionListener(e -> logout(user));
    }

    private void handleCreateGroup(ActionEvent e) {
        showGroupDialog();
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

    private boolean addGroup(String name, boolean online) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (User u : cached) {
            if (name.equals(u.getUsername())) {
                u.setStatus(online ? OnlineStatus.ONLINE : OnlineStatus.OFFLINE);
                userList.repaint();
                return false;
            }
        }
        User grp = new User(name, "", UserRole.GENERAL);
        grp.setStatus(online ? OnlineStatus.ONLINE : OnlineStatus.OFFLINE);
        cached.add(grp);
        if (userList.getModel() instanceof DefaultListModel) {
            DefaultListModel<User> mdl = (DefaultListModel<User>) userList.getModel();
            mdl.addElement(grp);
        }
        return true;
    }

    private void showMembers(String name) {
        List<User> mems = groups.get(name);
        if (mems == null || mems.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No members recorded for this group.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Members of ").append(name).append(":\n\n");
        User adm = mems.get(0);
        for (User m : mems) {
            sb.append("- ").append(m.getUsername());
            if (m.equals(adm)) {
                sb.append(" (admin)");
            }
            sb.append('\n');
        }
        JOptionPane.showMessageDialog(frame, sb.toString(), "Group Members", JOptionPane.INFORMATION_MESSAGE);
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().replaceAll("\\s+", "_").toLowerCase();
    }

    private boolean hasOtherOnline(List<User> mems) {
        if (mems == null) {
            return false;
        }
        for (User m : mems) {
            if (m == null || user == null) {
                continue;
            }
            if (!m.getUserID().equals(user.getUserID()) && m.getStatus() == OnlineStatus.ONLINE) {
                return true;
            }
        }
        return false;
    }

    private String register(ChatSession session) {
        if (session == null) {
            return "";
        }
        sessions.putIfAbsent(session.getChatID(), session);
        return session.getChatID();
    }

    private void registerKey(ChatSession session, String key) {
        if (session == null) {
            return;
        }
        String id = register(session);
        if (key != null && !key.isEmpty()) {
            keys.put(key, id);
        }
    }

    private void openFor(User contact) {
        if (contact == null) {
            return;
        }
        String name = contact.getUsername();
        boolean isGroupContact = groups.containsKey(name);
        String key = isGroupContact ? "group:" + normalize(name) : "priv:" + contact.getUserID();
        String id = keys.get(key);

        //Check if chat is already open
        if (id != null && isOpen(id)) {
            selectTab(id);
            return;
        }

        //Check if session exists but window not open
        if (id != null) {
            ChatSession sess = sessions.get(id);
            if (sess != null) {
                openChat(sess);
                return;
            }
        }

        //If no existing session, start a new chat
        if (isGroupContact) {
            //For group chats, request session from server
            List<User> mems = groups.get(name);
            if (mems != null && conn != null && conn.isConnected()) {
                conn.create(mems, true, name);
            }
        } else {
            //For private chats, request session from server
            startPrivate(contact);
        }
    }

    private void selectTab(String id) {
        JTextArea area = areas.get(id);
        if (area == null) {
            return;
        }
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component comp = tabs.getComponentAt(i);
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                if (panel.isAncestorOf(area)) {
                    tabs.setSelectedIndex(i);
                    return;
                }
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
                if (found != null) {
					return found;
				}
            }
        }
        return null;
    }

    private static class ContactRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof User) {
                User u = (User) value;
                label.setText(u.getUsername());
                label.setOpaque(true);
                label.setBackground(isSelected ? new Color(202, 231, 217) : new Color(236, 229, 221));
                label.setForeground(u.getStatus() == OnlineStatus.ONLINE ? new Color(37, 131, 59) : Color.BLACK);
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(207, 210, 207)),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)
                ));
            }
            return label;
        }
    }
}