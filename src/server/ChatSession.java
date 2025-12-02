package server;
<<<<<<< HEAD
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.Serializable;
=======

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import common.User;
import common.Message;
>>>>>>> cb168d1 (Pushed all latest changes)

public class ChatSession implements Serializable {
	
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
		this.chatName = chatName == null || chatName.isEmpty() ? "" : chatName;
		this.participants = new ArrayList<>(participants); 
		this.messages = new ArrayList<>();
		this.isGroup = isGroup; 
	}
	
	// Constructor used when loading an existing session from log file (to preserve chat ID)
	public ChatSession(String chatID, List<User> participants, boolean isGroup, String chatName) {
		this.chatID = chatID;
		this.chatName = chatName == null || chatName.isEmpty() ? "" : chatName;
		this.participants = new ArrayList<>(participants); 
		this.messages = new ArrayList<>(); 
		this.isGroup = isGroup; 
	}
	
	// Overloaded constructor with default empty chatName
	public ChatSession(List<User> participants, boolean isGroup) {
		this(participants, isGroup, "");
	}
	
	// Add a message to the chat 
	public void addMessage(Message message) {
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
	
	// Getters 
	public String getChatID() { 
		return chatID; 
	}
	
	public String getChatName() { 
		return chatName; 
	}
	
	public List<User> getParticipants() { 
		return new ArrayList<>(participants); 
	}
	
	public List<Message> getMessages() { 
		return new ArrayList<>(messages); 
	}
	
	public boolean isGroup() {
		return isGroup; 
	}
	
	public boolean isGroupChat() {
		return isGroup;
	}
}
