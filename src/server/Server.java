package server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import common.User;
import common.Message;
import common.UserRole;
import common.OnlineStatus;

public class Server {

    private static final int DEFAULT_PORT = 1234;
    private static ConnectionManager connectionManager;
    private static Authentication authentication;
    private static ChatManager chatManager;
    private static Logger logger;
    private static Map<String, User> onlineUsers;

    public static void main(String[] arguments) {
        int portNumber = DEFAULT_PORT;

        if (arguments.length > 0) {
            try {
                portNumber = Integer.parseInt(arguments[0]);
            } catch (NumberFormatException exception) {
                System.out.println("Invalid port number. Using default " + DEFAULT_PORT + ".");
            }
        }

        // Initialize components
        String credentialsFile = "data/credentials.txt";
        String logFile = "data/chat_log.txt";

        authentication = new Authentication(credentialsFile);
        logger = new Logger(logFile, credentialsFile);
        chatManager = new ChatManager(logger);
        connectionManager = new ConnectionManager();
        connectionManager.startManager();
        onlineUsers = new ConcurrentHashMap<>();

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server started on port: " + portNumber);
            System.out.println("Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());

                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectInputStream input;
        private ObjectOutputStream output;
        private User currentUser;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                output = new ObjectOutputStream(socket.getOutputStream());
                output.flush();
                input = new ObjectInputStream(socket.getInputStream());

                while (true) {
                    Object obj = input.readObject();

                    if (obj instanceof String) {
                        String command = (String) obj;
                        handleCommand(command);
                    } else if (obj instanceof Message) {
                        handleMessage((Message) obj);
                    } else if (obj instanceof SessionRequest) {
                        handleSessionRequest((SessionRequest) obj);
                    }
                }

            } catch (EOFException e) {
                System.out.println("Client disconnected: " + (currentUser != null ? currentUser.getUserID() : "unknown"));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                cleanup();
            }
        }

        private void handleCommand(String command) throws IOException {
            if (command.startsWith("LOGIN:")) {
                String[] parts = command.substring(6).split(":");
                System.out.println("Login attempt - parts length: " + parts.length);
                if (parts.length >= 2) {
                    String username = parts[0];
                    // Handle case where password might contain colons
                    String password = parts.length == 2 ? parts[1] : command.substring(6 + username.length() + 1);
                    System.out.println("Attempting login for user: " + username);
                    
                    User user = authentication.validateCredentials(username, password);
                    System.out.println("User validation result: " + (user != null ? "SUCCESS" : "FAILED"));
                    
                    if (user != null) {
                        boolean alreadyLoggedIn = authentication.checkStatus(user);
                        System.out.println("User already logged in: " + alreadyLoggedIn);
                        
                        if (!alreadyLoggedIn) {
                            String sessionID = authentication.createSession(user);
                            System.out.println("Session creation result: " + (sessionID != null ? "SUCCESS" : "FAILED"));
                            
                            if (sessionID != null) {
                                currentUser = user;
                                // Register the existing streams we already created
                                connectionManager.registerClientStreams(user, input, output, socket);
                                onlineUsers.put(user.getUserID(), user);
                                output.writeObject("LOGIN_SUCCESS:" + user.getUserID());
                                output.flush();
                                
                                // Send the actual User object with correct role
                                output.writeObject(user);
                                output.flush();
                                
                                // Send user's existing sessions
                                List<ChatSession> sessions = chatManager.loadUserSessions(user);
                                output.writeObject(sessions);
                                output.flush();
                                
                                // Send user list immediately to the newly logged-in user
                                List<User> users = new ArrayList<>(onlineUsers.values());
                                output.writeObject("USER_LIST_UPDATE");
                                output.writeObject(users);
                                output.flush();
                                
                                // Broadcast updated user list to all other users
                                broadcastUserList();
                                System.out.println("Login successful for: " + username);
                                return;
                            } else {
                                System.out.println("Session creation returned null");
                            }
                        } else {
                            System.out.println("User is already logged in");
                        }
                    } else {
                        System.out.println("Invalid credentials for: " + username);
                    }
                } else {
                    System.out.println("Invalid login command format. Expected LOGIN:username:password");
                }
                output.writeObject("LOGIN_FAILED");
                output.flush();
                
            } else if (command.equals("GET_USER_LIST")) {
                List<User> users = new ArrayList<>(onlineUsers.values());
                output.writeObject("USER_LIST_UPDATE");
                output.writeObject(users);
                output.flush();
                
            } else if (command.equals("GET_ALL_USERS")) {
                // Return all registered users from credentials file (for group creation)
                List<User> allUsers = authentication.getAllRegisteredUsers();
                output.writeObject("ALL_USERS_LIST");
                output.writeObject(allUsers);
                output.flush();
                
            } else if (command.equals("LOGOUT")) {
                if (currentUser != null) {
                    UserSession session = authentication.getSessionFor(currentUser);
                    if (session != null) {
                        authentication.endSession(session.getSessionID() + "");
                    }
                    connectionManager.disconnectClient(currentUser);
                    onlineUsers.remove(currentUser.getUserID());
                    broadcastUserList();
                }
                output.writeObject("LOGOUT_SUCCESS");
                output.flush();
            }
        }

        private void handleMessage(Message message) throws IOException {
            if (currentUser == null) return;

            // Add message to chat session and log it
            chatManager.receiveMessage(message);
            
            // Get the chat session to check if it's a group
            ChatSession session = chatManager.getChatSession(message.getChatID());
            if (session == null) {
                System.err.println("No session found for chatID: " + message.getChatID());
                return;
            }
            
            boolean isGroup = session.isGroup();
            List<User> targets = new ArrayList<>();

            // For group chats, send to ALL online participants (not just active viewers)
            if (isGroup) {
                List<User> allParticipants = session.getParticipants();
                Set<String> targetUserIDs = new java.util.HashSet<>();
                
                // Add all online participants who aren't the sender
                for (User participant : allParticipants) {
                    if (!participant.getUserID().equals(message.getSenderID())) {
                        // Check if participant is online
                        if (onlineUsers.containsKey(participant.getUserID())) {
                            // Use the online user object (has correct status)
                            User onlineParticipant = onlineUsers.get(participant.getUserID());
                            if (!targetUserIDs.contains(onlineParticipant.getUserID())) {
                                targets.add(onlineParticipant);
                                targetUserIDs.add(onlineParticipant.getUserID());
                            }
                        }
                    }
                }
            } else {
                // Private chat: send to the other participant if they're online
                for (User participant : session.getParticipants()) {
                    if (!participant.getUserID().equals(message.getSenderID())) {
                        if (onlineUsers.containsKey(participant.getUserID())) {
                            targets.add(onlineUsers.get(participant.getUserID()));
                        }
                    }
                }
            }

            // Send message to all targets
            for (User target : targets) {
                ObjectOutputStream targetOut = (ObjectOutputStream) connectionManager.getOutputStream(target);
                if (targetOut != null) {
                    try {
                        targetOut.writeObject(message);
                        targetOut.flush();
                        System.out.println("Sent message to " + target.getUserID());
                    } catch (IOException e) {
                        // User may have disconnected
                        System.err.println("Failed to send message to " + target.getUserID() + ": " + e.getMessage());
                    }
                }
            }

            // Also send back to sender for confirmation
            output.writeObject(message);
            output.flush();
        }

        private void handleSessionRequest(SessionRequest request) throws IOException {
            if (currentUser == null) return;

            List<User> participants = request.getParticipants();
            if (participants == null || participants.isEmpty()) {
                return;
            }

            // Ensure current user is in participants
            boolean hasCurrentUser = false;
            for (User p : participants) {
                if (p.getUserID().equals(currentUser.getUserID())) {
                    hasCurrentUser = true;
                    break;
                }
            }
            if (!hasCurrentUser) {
                participants.add(0, currentUser);
            }

            // For private chats, check if session already exists
            ChatSession session = null;
            if (!request.isGroup() && participants.size() == 2) {
                session = chatManager.findExistingPrivateSession(participants);
            }

            // Create new session if none exists
            if (session == null) {
                session = chatManager.createSession(participants, request.isGroup(), request.getChatName());
            }

            chatManager.joinSession(session.getChatID(), currentUser);

            // Load history and send to client
            List<Message> history = chatManager.loadHistory(session.getChatID());
            output.writeObject(session);
            output.writeObject(history);
            output.flush();

            // Notify other participants by sending them the session
            // Only send to online participants
            for (User participant : participants) {
                if (!participant.getUserID().equals(currentUser.getUserID())) {
                    // Check if participant is online
                    if (onlineUsers.containsKey(participant.getUserID())) {
                        User onlineParticipant = onlineUsers.get(participant.getUserID());
                        ObjectOutputStream targetOut = (ObjectOutputStream) connectionManager.getOutputStream(onlineParticipant);
                        if (targetOut != null) {
                            try {
                                // Send the session and empty history to other participants
                                targetOut.writeObject(session);
                                targetOut.writeObject(new ArrayList<Message>());
                                targetOut.flush();
                                System.out.println("Sent session notification to " + onlineParticipant.getUserID());
                            } catch (IOException e) {
                                // Participant may have disconnected
                                System.err.println("Failed to notify " + onlineParticipant.getUserID() + ": " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }

        private void broadcastUserList() {
            List<User> users = new ArrayList<>(onlineUsers.values());
            for (User user : onlineUsers.values()) {
                ObjectOutputStream out = (ObjectOutputStream) connectionManager.getOutputStream(user);
                if (out != null) {
                    try {
                        out.writeObject("USER_LIST_UPDATE");
                        out.writeObject(users);
                        out.flush();
                    } catch (IOException e) {
                        // User may have disconnected
                    }
                }
            }
        }

        private void cleanup() {
            if (currentUser != null) {
                UserSession session = authentication.getSessionFor(currentUser);
                if (session != null) {
                    authentication.endSession(session.getSessionID() + "");
                }
                connectionManager.disconnectClient(currentUser);
                onlineUsers.remove(currentUser.getUserID());
                broadcastUserList();
            }
            try {
                if (input != null) input.close();
                if (output != null) output.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Helper class for session creation requests
    public static class SessionRequest implements java.io.Serializable {
        private List<User> participants;
        private boolean isGroup;
        private String chatName;

        public SessionRequest(List<User> participants, boolean isGroup, String chatName) {
            this.participants = participants;
            this.isGroup = isGroup;
            this.chatName = chatName;
        }

        public List<User> getParticipants() { return participants; }
        public boolean isGroup() { return isGroup; }
        public String getChatName() { return chatName; }
    }
}
