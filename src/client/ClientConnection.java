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
    private Socket sock;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String host;
    private int port;
    private AtomicBoolean conn;
    private Thread thread;
    private MessageListener listener;
    private User user;

    public interface MessageListener {
        void onMessageReceived(Message message);
        void onUserListUpdated(List<User> users);
        void onAllUsersReceived(List<User> users);
        void onSessionReceived(ChatSession session, List<Message> history);
        void onNewSessionNotification(String chatID);
        void onChatLogsReceived(String logContent);
    }

    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
        this.conn = new AtomicBoolean(false);
    }

    public boolean connect() {
        try {
            sock = new Socket(host, port);
            out = new ObjectOutputStream(sock.getOutputStream());
            out.flush();
            in = new ObjectInputStream(sock.getInputStream());
            conn.set(true);
            //Don't start listener thread yet - wait until after login

            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    public void startListener() {
        if (thread == null || !thread.isAlive()) {
            thread = new Thread(this::listen);
            thread.setDaemon(true);
            thread.start();
            System.out.println("Client: Started message listener thread");
        }
    }

    public boolean login(String username, String password) {
        System.out.println("Client: Starting login for user: " + username);

        if (!conn.get()) {
            System.out.println("Client: Not connected, attempting to connect...");
            if (!connect()) {
                System.out.println("Client: Connection failed");
                return false;
            }
            System.out.println("Client: Connected successfully");
        }

        try {
            System.out.println("Client: Sending login command...");
            out.writeObject("LOGIN:" + username + ":" + password);
            out.flush();
            System.out.println("Client: Login command sent, waiting for response...");

            Object resp = in.readObject();
            System.out.println("Client: Received response: " + resp + " (type: " + (resp != null ? resp.getClass().getName() : "null") + ")");

            if (resp instanceof String && ((String) resp).startsWith("LOGIN_SUCCESS:")) {
                System.out.println("Client: LOGIN_SUCCESS received!");
                try {
                    //Read the User object from server
                    Object uObj = in.readObject();
                    if (uObj instanceof User) {
                        this.user = (User) uObj;
                        System.out.println("Client: Received user object: " + this.user.getUsername());
                    } else {
                        System.out.println("Client: Warning - Expected User object but got: " + (uObj != null ? uObj.getClass().getName() : "null"));
                    }

                    //Read sessions
                    Object sessObj = in.readObject();
                    if (sessObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<ChatSession> sess = (List<ChatSession>) sessObj;
                        System.out.println("Client: Received " + sess.size() + " sessions");

                        //Process sessions if listener is set
                        if (listener != null) {
                            for (ChatSession s : sess) {
                                List<Message> hist = new ArrayList<>();
                                listener.onSessionReceived(s, hist);
                            }
                        }
                    } else {
                        System.out.println("Client: Warning - Expected List but got: " + (sessObj != null ? sessObj.getClass().getName() : "null"));
                    }
                    System.out.println("Client: Login successful");
                    //Don't start listener thread here - let ClientGUI start it after setting message listener
                    return true;
                } catch (Exception e) {
                    System.err.println("Client: Error reading login response data: " + e.getMessage());
                    e.printStackTrace();
                    //Still return true if we got LOGIN_SUCCESS and at least the user object
                    if (this.user != null) {
                        return true;
                    }
                    return false;
                }
            } else if (resp instanceof String && ((String) resp).equals("LOGIN_FAILED")) {
                System.out.println("Client: Login failed - server rejected credentials");
                return false;
            } else {
                System.out.println("Client: Unexpected response: " + resp + " (class: " + (resp != null ? resp.getClass().getName() : "null") + ")");
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

    public void send(Message msg) {
        if (!conn.get() || out == null) {
			return;
		}

        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }

    public void requestUsers() {
        if (!conn.get() || out == null) {
			return;
		}

        try {
            out.writeObject("GET_USER_LIST");
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to request user list: " + e.getMessage());
        }
    }

    public void requestAll() {
        if (!conn.get() || out == null) {
			return;
		}

        try {
            out.writeObject("GET_ALL_USERS");
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to request all users: " + e.getMessage());
        }
    }

    public void requestLogs() {
        if (!conn.get() || out == null) {
			return;
		}

        try {
            out.writeObject("GET_CHAT_LOGS");
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to request chat logs: " + e.getMessage());
        }
    }

    public void create(List<User> parts, boolean isGrp, String name) {
        if (!conn.get() || out == null) {
			return;
		}

        try {
            server.Server.SessionRequest req = new server.Server.SessionRequest(parts, isGrp, name);
            out.writeObject(req);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to create session: " + e.getMessage());
        }
    }

    public void logout() {
        if (!conn.get() || out == null) {
			return;
		}

        try {
            out.writeObject("LOGOUT");
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to logout: " + e.getMessage());
        }
        disconnect();
    }

    public void disconnect() {
        conn.set(false);
        try {
            if (in != null) {
				in.close();
			}
            if (out != null) {
				out.close();
			}
            if (sock != null) {
				sock.close();
			}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setListener(MessageListener lst) {
        this.listener = lst;
    }

    private void listen() {
        try {
            while (conn.get()) {
                Object obj = in.readObject();

                if (obj instanceof Message) {
                    if (listener != null) {
                        listener.onMessageReceived((Message) obj);
                    }
                } else if (obj instanceof String) {
                    String cmd = (String) obj;
                    if (cmd.equals("USER_LIST_UPDATE")) {
                        Object uObj = in.readObject();
                        if (uObj instanceof List && listener != null) {
                            @SuppressWarnings("unchecked")
                            List<User> us = (List<User>) uObj;
                            listener.onUserListUpdated(us);
                        }
                    } else if (cmd.equals("ALL_USERS_LIST")) {
                        Object uObj = in.readObject();
                        if (uObj instanceof List && listener != null) {
                            @SuppressWarnings("unchecked")
                            List<User> us = (List<User>) uObj;
                            listener.onAllUsersReceived(us);
                        }
                    } else if (cmd.equals("CHAT_LOGS_DATA")) {
                        Object logObj = in.readObject();
                        if (logObj instanceof String && listener != null) {
                            listener.onChatLogsReceived((String) logObj);
                        }
                    } else if (cmd.equals("LOG_ACCESS_DENIED")) {
                        if (listener != null) {
                            listener.onChatLogsReceived("Access denied: Admin privileges required.");
                        }
                    } else if (cmd.startsWith("NEW_SESSION:")) {
                        String id = cmd.substring(12);
                        if (listener != null) {
                            listener.onNewSessionNotification(id);
                        }
                    }
                } else if (obj instanceof List) {
                    //Handle direct user list response (for GET_USER_LIST)
                    if (listener != null) {
                        @SuppressWarnings("unchecked")
                        List<User> us = (List<User>) obj;
                        listener.onUserListUpdated(us);
                    }
                } else if (obj instanceof ChatSession) {
                    ChatSession sess = (ChatSession) obj;
                    Object histObj = in.readObject();
                    if (histObj instanceof List && listener != null) {
                        @SuppressWarnings("unchecked")
                        List<Message> hist = (List<Message>) histObj;
                        listener.onSessionReceived(sess, hist);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (conn.get()) {
                System.err.println("Connection lost: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return conn.get();
    }

    public User getUser() {
        return user;
    }
}

