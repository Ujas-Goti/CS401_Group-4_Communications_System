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
    public User validateCredentials(String username, String password) {

        try (BufferedReader reader = new BufferedReader(new FileReader(credentialFile))) {
            String line;

            while ((line = reader.readLine()) != null) {
                
                // Expected format: userID,username,password,role
                String[] parts = line.split(",");

                if (parts.length < 4) {
                    continue;
                }

                String fileUserID = parts[0].trim();
                String fileUsername = parts[1].trim();
                String filePassword = parts[2].trim();
                String fileRole = parts[3].trim();

                if (fileUsername.equals(username) && filePassword.equals(password)) {
                    // Create a User object 
                    User.UserRole role = User.UserRole.valueOf(fileRole.toUpperCase());
                    User user = new User(fileUserID, fileUsername, filePassword, role);

                    // Mark as online 
                    user.setStatus(User.OnlineStatus.ONLINE);
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
                session.getUser().setStatus(User.OnlineStatus.OFFLINE);
                return true;
            }
        }

        return false;
    }

    
    // Returns true if user is already in an active session
    public synchronized boolean checkStatus(User user) {
        for (UserSession session : activeSessions) {
            if (session.getUser().getUserId().equals(user.getUserId())) {
                return true;
            }
        }
        return false;
    }

    
    // Getter for session by user ID if needed
    public synchronized UserSession getSessionFor(User user) {
        for (UserSession session : activeSessions) {
            if (session.getUser().getUserId().equals(user.getUserId())) {
                return session;
            }
        }
        return null;
    }
}

