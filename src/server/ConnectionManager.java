import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import common.User;
import common.OnlineStatus;

public class ConnectionManager {
    private final ExecutorService threadPool;
    private final Map<String, Socket> clientSockets;
    private final Map<String, ObjectInputStream> clientInput;
    private final Map<String, ObjectOutputStream> clientOutput;
    private final AtomicBoolean running;

    public ConnectionManager() {
        this.threadPool = Executors.newCachedThreadPool();
        this.clientSockets = new ConcurrentHashMap<>();
        this.clientInput = new ConcurrentHashMap<>();
        this.clientOutput = new ConcurrentHashMap<>();
        this.running = new AtomicBoolean(false);
    }

    public void startManager() {
        running.set(true);
    }

    public void registerClient(User user, Socket socket) {
        ensureRunning();
        if (user == null || socket == null) {
            throw new IllegalArgumentException("User and socket must be provided");
        }
        String key = userKey(user);
        disconnectClient(user);
        threadPool.submit(() -> initializeClient(key, socket, user));
    }
    
    // Register client with existing streams (to avoid creating duplicate streams)
    public void registerClientStreams(User user, ObjectInputStream input, ObjectOutputStream output, Socket socket) {
        ensureRunning();
        if (user == null || socket == null || input == null || output == null) {
            throw new IllegalArgumentException("User, socket, and streams must be provided");
        }
        String key = userKey(user);
        disconnectClient(user);
        clientSockets.put(key, socket);
        clientOutput.put(key, output);
        clientInput.put(key, input);
        user.setStatus(OnlineStatus.ONLINE);
    }

    public void disconnectClient(User user) {
        if (user == null) {
            return;
        }
        String key = userKey(user);
        ObjectInputStream in = clientInput.remove(key);
        ObjectOutputStream out = clientOutput.remove(key);
        Socket socket = clientSockets.remove(key);
        closeQuietly(in);
        closeQuietly(out);
        closeSocket(socket);
        user.setStatus(OnlineStatus.OFFLINE);
    }

    public OutputStream getOutputStream(User user) {
        if (user == null) {
            return null;
        }
        return clientOutput.get(userKey(user));
    }

    public InputStream getInputStream(User user) {
        if (user == null) {
            return null;
        }
        return clientInput.get(userKey(user));
    }

    private void initializeClient(String key, Socket socket, User user) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            clientSockets.put(key, socket);
            clientOutput.put(key, out);
            clientInput.put(key, in);
            user.setStatus(OnlineStatus.ONLINE);
        } catch (IOException e) {
            closeSocket(socket);
            throw new RuntimeException("Failed to register client", e);
        }
    }

    private void ensureRunning() {
        if (!running.get()) {
            throw new IllegalStateException("ConnectionManager has not started");
        }
    }

    private String userKey(User user) {
        String id = user.getUserID();
        if (id != null && !id.isEmpty()) {
            return id;
        }
        String username = user.getUsername();
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("User has no identifier");
        }
        return username;
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    private void closeSocket(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
