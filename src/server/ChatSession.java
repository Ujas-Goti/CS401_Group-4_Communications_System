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
	private String.generateChatID(){
		return UUID.randomUUID().toString 
	}
	
	// Constructors for a group chat
	public ChatSession(String chatID, String chatName, List<User> participants) {
		this.chatID = chatID;
		this.chatName = chatName;
		this.participants = new ArrayList<>(); 
		this.messages = new ArrayList<>();
		this.isGroup = this.participants.size() > 2; // more than two participants is a group chat 
	}
	
	// Constructors for a private chat, aka 1 to 1 chat    
	public ChatSession(String chatID, User user1, User user2) {
		this.chatID = chatID;
		this.chatName = chatName;
		this.participants = new ArrayList<>();
		this.participants.add(user1);
		this.participants.add(user2); 
		this.messages = new ArrayList<>(); 
		this.isGroup = False; 
	}
	
	// Add a message to the chat 
	public void addMessageToChat (Message message) {
		messages.add(message); 
	}
	
	// Add a user to the a group chat 
	public void addParticipants (User user) {
		if (!participants.contains(user){
			partcipants.add(user);
			if (participants.size() > 2) {
				isGroup = True;
				
			}
		}
	}
	
	// Getters 
	public String getChatID() { return chatID; }
	public String getChatName() { return chatName; }
	public List<User> getParticipants() { return participants; }
	public List<Message> getMessages() { return messages; }
	public boolean isGroup() {return isGroup; } 
	

}
