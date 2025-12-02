package server;
import java.io.*;
import java.util.*;

public class Authentication {

    private final String credentialFile;
    private final List<UserSession> activeSessions;

    public Authentication(String credentialFile) {
        this.credentialFile = credentialFile;
        this.activeSessions = Collections.synchronizedList(new ArrayList<>());
    }

    // Validate username/password against the credential text file
    // Expected file format: username,password,role
    public User validateCredentials(String username, String password) {

        try (BufferedReader reader = new BufferedReader(new FileReader(credentialFile))) {
            String line;

            while ((line = reader.readLine()) != null) {
                
                // Expected format: username,password,role
                String[] parts = line.split(",");

                if (parts.length < 3) {
                    continue;
                }

                String fileUsername = parts[0].trim();
                String filePassword = parts[1].trim();
                String fileRole = parts[2].trim();

                if (fileUsername.equals(username) && filePassword.equals(password)) {
                    // Create a User object with role from UserRole enum
                    UserRole role = UserRole.valueOf(fileRole.toUpperCase());
                    User user = new User(fileUsername, filePassword, role);
                    // userID is automatically set to username in User constructor

                    // Mark as online 
                    user.setStatus(OnlineStatus.ONLINE);
                    return user;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null; 	// failed authentication
    }

    // Creates a new session for the user if not already logged in
    // Returns sessionID as string
    public synchronized String createSession(User user) {

        if (checkStatus(user)) {
            return null; 	// user is already logged in
        }

        UserSession session = new UserSession(user);
        activeSessions.add(session);
        return Integer.toString(session.getSessionID());
    }

    // Removes a session based on its ID
    public synchronized boolean endSession(String sessionID) {

        Iterator<UserSession> it = activeSessions.iterator();

        while (it.hasNext()) {
            UserSession session = it.next();
            if (Integer.toString(session.getSessionID()).equals(sessionID)) {
                it.remove();
                session.getUser().setStatus(OnlineStatus.OFFLINE);
                return true;
            }
        }

        return false;
    }

    // Returns true if user is already in an active session
    public synchronized boolean checkStatus(User user) {
        for (UserSession session : activeSessions) {
            if (session.getUser().getUserID().equals(user.getUserID())) {
                return true;
            }
        }
        return false;
    }

    // Getter for session by user ID if needed
    public synchronized UserSession getSessionFor(User user) {
        for (UserSession session : activeSessions) {
            if (session.getUser().getUserID().equals(user.getUserID())) {
                return session;
            }
        }
        return null;
    }
}