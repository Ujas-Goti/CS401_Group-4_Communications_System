package server;
<<<<<<< HEAD
=======

>>>>>>> cb168d1 (Pushed all latest changes)
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import common.User;
import common.Message;

public class ChatManager {

    //	all chat sessions that are currently active in memory
    private final Map<String, ChatSession> activeChatSessions;

    //	maps chatID -> users who currently have that chat window open
    private final Map<String, Set<User>> activeViewers;

    private final Logger logger;

    
    public ChatManager(Logger logger) {
        this.activeChatSessions = new ConcurrentHashMap<>();
        this.activeViewers = new ConcurrentHashMap<>();
        this.logger = logger;
    }

    
    
    //	creates a new chat session
    public synchronized ChatSession createSession(List<User> participants, boolean isGroup, String chatName) {
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participants list cannot be empty");
        }

        ChatSession session = new ChatSession(participants, isGroup, chatName);
        activeChatSessions.put(session.getChatID(), session);

        //	initializes active viewers list. starts empty
        activeViewers.put(session.getChatID(), ConcurrentHashMap.newKeySet());
        
        logger.logSession(session);

        return session;
    }

 
    //	ClientHandler calls this when a user opens a chat window in ClientGUI
    public void joinSession(String chatID, User user) {
    	if (chatID == null || user == null) return;
    	
        Set<User> viewers = activeViewers.get(chatID);

        // if there doesn't exist a set of viewers for this chatID, then starts one
        if (viewers == null) {
            viewers = ConcurrentHashMap.newKeySet();
            activeViewers.put(chatID, viewers);
        }

        // add user to the viewers set
        viewers.add(user);
    }


    
    //	ClientHandler calls this when a user closes a chat window in ClientGUI
    public void leaveSession(String chatID, User user) {
    	if (chatID == null || user == null) return;
        Set<User> viewers = activeViewers.get(chatID);
        if (viewers != null) {
            viewers.remove(user);
        }
    }

    
    
    //	Server or ClientHandler calls this when a user sends a message
    //	it (1) stores msg in ChatSession; (2) calls logger to log msg; (3) determines targets for ClientHandler to send msg to
    public synchronized List<User> receiveMessage(Message msg) {

        if (msg == null) return Collections.emptyList();

        ChatSession session = activeChatSessions.get(msg.getChatID());
        if (session == null) {
            System.err.println("ChatManager: No session found for chatID=" + msg.getChatID());
            return Collections.emptyList();
        }

        // store the message
        session.addMessage(msg);

        // log to file
        logger.logMessage(msg);

        // get all users currently viewing this chat window
        Set<User> active = activeViewers.getOrDefault(msg.getChatID(), Collections.emptySet());

        List<User> targets = new ArrayList<>();

        // loop through each active viewer
        for (User viewer : active) {

            // skip the sender
            if (viewer.getUserID().equals(msg.getSenderID())) {
                continue;
            }

            // check if this viewer is actually part of the chat
            boolean isParticipant = false;
            for (User p : session.getParticipants()) {
                if (p.getUserID().equals(viewer.getUserID())) {
                    isParticipant = true;
                    break;
                }
            }

            // only send if they are part of the chat
            if (isParticipant) {
                targets.add(viewer);
            }
        }
        return targets;
    }



    
    //	optional method... just another way of calling recieve message
    public synchronized void sendMessage(String chatID, Message msg) {
        receiveMessage(msg);
    }


    
    
    //	loads message history for a single chatSession from LogFile
    //	this gets called when user opens chat window for first time
    public List<Message> loadHistory(String chatID) {
    	List<Message> history = logger.getMessagesForChat(chatID);
    	ChatSession session = getChatSession(chatID);
    	session.getMessages().addAll(history);
        return history;
    }

    
    //	loads all the chatSessions a single user is a part of
    //	this is used when loading the client's list of chats
    public List<ChatSession> loadUserSessions(User user) {
    	if (user == null) return Collections.emptyList();
    	
        List<ChatSession> userSessions = new ArrayList<>();

        // get sessions already in memory
        for (ChatSession session : activeChatSessions.values()) {
            for (User u : session.getParticipants()) {
                if (u.getUserID().equals(user.getUserID())) {
                    userSessions.add(session);
                    break;
                }
            }
        }

        // load sessions from log file that aren't in memory yet
        List<String> sessionLines = logger.filterSessionsByUser(user.getUserID());
        for (String line : sessionLines) {
            String[] parts = line.split("\\|");
            if (parts.length < 4) continue;

            String chatID = parts[0];
            if (activeChatSessions.containsKey(chatID)) continue;

            // load participants
            List<User> participants = new ArrayList<>();
            String[] uids = parts[3].split(",");
            for (String uid : uids) {
                User u = logger.loadUserByID(uid);
                if (u != null) participants.add(u);
            }

            // create session and add to memory & result
            if (participants.size() >= 2) {
                // create session preserving the chatID from file
            	boolean isGroup = Boolean.parseBoolean(parts[2]);
            	String chatName = parts.length > 4 ? parts[4] : "";
            	ChatSession session = new ChatSession(chatID, participants, isGroup, chatName);
                activeChatSessions.put(chatID, session);
                activeViewers.putIfAbsent(chatID, ConcurrentHashMap.newKeySet());
                userSessions.add(session);
            }
        }
        return userSessions;
    }
    
    
    // Find existing private session between two users
    public ChatSession findExistingPrivateSession(List<User> participants) {
        if (participants == null || participants.size() != 2) {
            return null;
        }
        
        String user1ID = participants.get(0).getUserID();
        String user2ID = participants.get(1).getUserID();
        
        // Check active sessions
        for (ChatSession session : activeChatSessions.values()) {
            if (!session.isGroup() && session.getParticipants().size() == 2) {
                List<User> sessParticipants = session.getParticipants();
                String sessUser1 = sessParticipants.get(0).getUserID();
                String sessUser2 = sessParticipants.get(1).getUserID();
                
                if ((sessUser1.equals(user1ID) && sessUser2.equals(user2ID)) ||
                    (sessUser1.equals(user2ID) && sessUser2.equals(user1ID))) {
                    return session;
                }
            }
        }
        
        return null;
    }
    
    //	Getters
    public ChatSession getChatSession(String chatID) {
        return activeChatSessions.get(chatID);
    }

    public List<ChatSession> getActiveChatSessions() {
        return new ArrayList<>(activeChatSessions.values());
    }
}
