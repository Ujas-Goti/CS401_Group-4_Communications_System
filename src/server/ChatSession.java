<<<<<<< Current (Your changes)
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatSession {
    private String chatID;
    private String chatName;
    private List<User> participants;
    private List<Message> messages;
    private boolean isGroup;
    
    // Constructor matching class diagram: ChatSession(List<User> participants, isGroup: boolean, chatName: String="")
    public ChatSession(List<User> participants, boolean isGroup, String chatName) {
        this.chatID = UUID.randomUUID().toString();
        this.chatName = chatName == null || chatName.isEmpty() ? "" : chatName;
        this.participants = new ArrayList<>(participants);
        this.messages = new ArrayList<>();
        this.isGroup = isGroup;
    }
    
    // Overloaded constructor with default empty chatName
    public ChatSession(List<User> participants, boolean isGroup) {
        this(participants, isGroup, "");
    }
    
    // Add a participant to the chat
    public void addParticipant(User user) {
        if (user != null && !participants.contains(user)) {
            participants.add(user);
            // Auto-update isGroup if more than 2 participants
            if (participants.size() > 2) {
                isGroup = true;
            }
        }
    }
    
    // Remove a participant from the chat
    public void removeParticipant(User user) {
        if (user != null) {
            participants.remove(user);
            // Auto-update isGroup if 2 or fewer participants
            if (participants.size() <= 2) {
                isGroup = false;
            }
        }
    }
    
    // Add a message to the chat
    public void addMessage(Message message) {
        if (message != null) {
            messages.add(message);
        }
    }
    
    // Getters matching class diagram
    public String getChatID() {
        return chatID;
    }
    
    public String getChatName() {
        return chatName;
    }
    
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }
    
    public List<User> getParticipants() {
        return new ArrayList<>(participants);
    }
    
    public boolean isGroupChat() {
        return isGroup;
    }
    
    // Additional getter for compatibility
    public boolean isGroup() {
        return isGroup;
    }
}
=======
>>>>>>> Incoming (Background Agent changes)
