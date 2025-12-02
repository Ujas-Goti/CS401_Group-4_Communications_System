package server;
<<<<<<< HEAD
=======

>>>>>>> cb168d1 (Pushed all latest changes)
import java.io.*;
import java.util.*;
import common.User;
import common.UserRole;
import common.OnlineStatus;

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
        if (username == null || password == null || username.trim().isEmpty()) {
            System.out.println("Authentication: Invalid input - username or password is null/empty");
            return null;
        }

        File file = new File(credentialFile);
        if (!file.exists()) {
            System.err.println("ERROR: Credentials file not found at: " + file.getAbsolutePath());
            return null;
        }
        System.out.println("Reading credentials from: " + file.getAbsolutePath());

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Expected format: username,password,role
                String[] parts = line.split(",");

                if (parts.length < 3) {
                    continue;
                }

                String fileUsername = parts[0].trim();
                String filePassword = parts[1].trim();
                String fileRole = parts[2].trim();

                System.out.println("Checking credentials - File: [" + fileUsername + "] vs Input: [" + username.trim() + "]");
                System.out.println("File password length: " + filePassword.length() + ", Input password length: " + password.length());
                System.out.println("Password match: " + filePassword.equals(password.trim()));

                if (fileUsername.equals(username) && filePassword.equals(password)) {
                    // Create a User object with role from UserRole enum
                    UserRole role = UserRole.valueOf(fileRole.toUpperCase());
                    User user = new User(fileUsername, filePassword, role);
                    // userID is automatically set to username in User constructor

                    // Mark as online 
                    user.setStatus(OnlineStatus.ONLINE);
                    System.out.println("Authentication successful for: " + username);
                    return user;
                }
            }

        } catch (IOException e) {
            System.err.println("ERROR reading credentials file: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: Invalid role in credentials file: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Authentication failed for: " + username);
        return null; 	// failed authentication
    }
    
    // Get all registered users from credentials file (for group creation)
    public List<User> getAllRegisteredUsers() {
        List<User> allUsers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(credentialFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 3) {
                    continue;
                }
                String username = parts[0].trim();
                String password = parts[1].trim();
                String role = parts[2].trim();
                try {
                    UserRole userRole = UserRole.valueOf(role.toUpperCase());
                    User user = new User(username, password, userRole);
                    allUsers.add(user);
                } catch (IllegalArgumentException e) {
                    // Skip invalid roles
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR reading credentials file for all users: " + e.getMessage());
        }
        return allUsers;
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