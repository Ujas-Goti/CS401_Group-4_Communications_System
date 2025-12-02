import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

 

public class ChatSession {
	
	private String chatID;
	private String chatName;
	private List<User> participants;
	private List<Message> messages;
	private boolean isGroup;
	
	
	// Creating a unique ID for each chat 
	private String generateChatID(){
		return UUID.randomUUID().toString();
	}
	
	// Constructor for brand new session
	public ChatSession(List<User> participants, boolean isGroup, String chatName) {
		this.chatID = generateChatID();
		this.chatName = chatName;
		this.participants = new ArrayList<>(participants); 
		this.messages = new ArrayList<>();
		this.isGroup = isGroup; 
	}
	
	// Constructor used when loading an existing session from log file (to preserve chat ID)
	public ChatSession(String chatID, List<User> participants, boolean isGroup, String chatName) {
		this.chatID = chatID;
		this.chatName = chatName;
		this.participants = new ArrayList<>(participants); 
		this.messages = new ArrayList<>(); 
		this.isGroup = isGroup; 
	}
	
	// Add a message to the chat 
	public void addMessage (Message message) {
		if (message == null) return;
		messages.add(message); 
	}
	
	// Add a user to a group chat 
	public void addParticipant(User user) {
		if (user == null) return;
		
        if (!participants.contains(user)) {
            participants.add(user);
        }
        if (participants.size() > 2) {
            isGroup = true;
        }
    }

	
	// Getters 
	public String getChatID() { return chatID; }
	public String getChatName() { return chatName; }
	public List<User> getParticipants() { return participants; }
	public List<Message> getMessages() { return messages; }
	public boolean isGroup() {return isGroup; } 
	

}
