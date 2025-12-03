package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import common.Message;
import common.User;
import server.ChatSession;

public class ClientConnection {
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String serverHost;
    private int serverPort;
    private AtomicBoolean connected;
    private Thread listenerThread;
    private MessageListener messageListener;
    private User loggedInUser;

    public interface MessageListener {
        void onMessageReceived(Message message);
        void onUserListUpdated(List<User> users);
        void onAllUsersReceived(List<User> users);
        void onSessionReceived(ChatSession session, List<Message> history);
        void onNewSessionNotification(String chatID);
    }

    public ClientConnection(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.connected = new AtomicBoolean(false);
    }

    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            connected.set(true);
            // Don't start listener thread yet - wait until after login

            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    public void startListenerThread() {
        if (listenerThread == null || !listenerThread.isAlive()) {
            listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();
            System.out.println("Client: Started message listener thread");
        }
    }

    public boolean login(String username, String password) {
        System.out.println("Client: Starting login for user: " + username);

        if (!connected.get()) {
            System.out.println("Client: Not connected, attempting to connect...");
            if (!connect()) {
                System.out.println("Client: Connection failed");
                return false;
            }
            System.out.println("Client: Connected successfully");
        }

        try {
            System.out.println("Client: Sending login command...");
            output.writeObject("LOGIN:" + username + ":" + password);
            output.flush();
            System.out.println("Client: Login command sent, waiting for response...");

            Object response = input.readObject();
            System.out.println("Client: Received response: " + response + " (type: " + (response != null ? response.getClass().getName() : "null") + ")");

            if (response instanceof String && ((String) response).startsWith("LOGIN_SUCCESS:")) {
                System.out.println("Client: LOGIN_SUCCESS received!");
                try {
                    // Read the User object from server
                    Object userObj = input.readObject();
                    if (userObj instanceof User) {
                        this.loggedInUser = (User) userObj;
                        System.out.println("Client: Received user object: " + this.loggedInUser.getUsername());
                    } else {
                        System.out.println("Client: Warning - Expected User object but got: " + (userObj != null ? userObj.getClass().getName() : "null"));
                    }

                    // Read sessions
                    Object sessionsObj = input.readObject();
                    if (sessionsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<ChatSession> sessions = (List<ChatSession>) sessionsObj;
                        System.out.println("Client: Received " + sessions.size() + " sessions");

                        // Process sessions if listener is set
                        if (messageListener != null) {
                            for (ChatSession session : sessions) {
                                List<Message> history = new ArrayList<>();
                                messageListener.onSessionReceived(session, history);
                            }
                        }
                    } else {
                        System.out.println("Client: Warning - Expected List but got: " + (sessionsObj != null ? sessionsObj.getClass().getName() : "null"));
                    }
                    System.out.println("Client: Login successful");
                    // Don't start listener thread here - let ClientGUI start it after setting message listener
                    return true;
                } catch (Exception e) {
                    System.err.println("Client: Error reading login response data: " + e.getMessage());
                    e.printStackTrace();
                    // Still return true if we got LOGIN_SUCCESS and at least the user object
                    if (this.loggedInUser != null) {
                        return true;
                    }
                    return false;
                }
            } else if (response instanceof String && ((String) response).equals("LOGIN_FAILED")) {
                System.out.println("Client: Login failed - server rejected credentials");
                return false;
            } else {
                System.out.println("Client: Unexpected response: " + response + " (class: " + (response != null ? response.getClass().getName() : "null") + ")");
                return false;
            }
        } catch (IOException e) {
            System.err.println("Client: IOException during login: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            System.err.println("Client: ClassNotFoundException during login: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Client: Unexpected exception during login: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void sendMessage(Message message) {
        if (!connected.get() || output == null) {
			return;
		}

        try {
            output.writeObject(message);
            output.flush();
        } catch (IOException e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }

    public void requestUserList() {
        if (!connected.get() || output == null) {
			return;
		}

        try {
            output.writeObject("GET_USER_LIST");
            output.flush();
        } catch (IOException e) {
            System.err.println("Failed to request user list: " + e.getMessage());
        }
    }

    public void requestAllUsers() {
        if (!connected.get() || output == null) {
			return;
		}

        try {
            output.writeObject("GET_ALL_USERS");
            output.flush();
        } catch (IOException e) {
            System.err.println("Failed to request all users: " + e.getMessage());
        }
    }

    public void createSession(List<User> participants, boolean isGroup, String chatName) {
        if (!connected.get() || output == null) {
			return;
		}

        try {
            server.Server.SessionRequest request = new server.Server.SessionRequest(participants, isGroup, chatName);
            output.writeObject(request);
            output.flush();
        } catch (IOException e) {
            System.err.println("Failed to create session: " + e.getMessage());
        }
    }

    public void logout() {
        if (!connected.get() || output == null) {
			return;
		}

        try {
            output.writeObject("LOGOUT");
            output.flush();
        } catch (IOException e) {
            System.err.println("Failed to logout: " + e.getMessage());
        }
        disconnect();
    }

    public void disconnect() {
        connected.set(false);
        try {
            if (input != null) {
				input.close();
			}
            if (output != null) {
				output.close();
			}
            if (socket != null) {
				socket.close();
			}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    private void listenForMessages() {
        try {
            while (connected.get()) {
                Object obj = input.readObject();

                if (obj instanceof Message) {
                    if (messageListener != null) {
                        messageListener.onMessageReceived((Message) obj);
                    }
                } else if (obj instanceof String) {
                    String command = (String) obj;
                    if (command.equals("USER_LIST_UPDATE")) {
                        Object usersObj = input.readObject();
                        if (usersObj instanceof List && messageListener != null) {
                            @SuppressWarnings("unchecked")
                            List<User> users = (List<User>) usersObj;
                            messageListener.onUserListUpdated(users);
                        }
                    } else if (command.equals("ALL_USERS_LIST")) {
                        Object usersObj = input.readObject();
                        if (usersObj instanceof List && messageListener != null) {
                            @SuppressWarnings("unchecked")
                            List<User> users = (List<User>) usersObj;
                            messageListener.onAllUsersReceived(users);
                        }
                    } else if (command.startsWith("NEW_SESSION:")) {
                        String chatID = command.substring(12);
                        if (messageListener != null) {
                            messageListener.onNewSessionNotification(chatID);
                        }
                    }
                } else if (obj instanceof List) {
                    // Handle direct user list response (for GET_USER_LIST)
                    if (messageListener != null) {
                        @SuppressWarnings("unchecked")
                        List<User> users = (List<User>) obj;
                        messageListener.onUserListUpdated(users);
                    }
                } else if (obj instanceof ChatSession) {
                    ChatSession session = (ChatSession) obj;
                    Object historyObj = input.readObject();
                    if (historyObj instanceof List && messageListener != null) {
                        @SuppressWarnings("unchecked")
                        List<Message> history = (List<Message>) historyObj;
                        messageListener.onSessionReceived(session, history);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (connected.get()) {
                System.err.println("Connection lost: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }
}

