import java.time.LocalDateTime;

public class Message {
	private String messageID;
	private String chatID;
	private String senderID;
	private LocalDateTime timeStamp;
	private String content;
	private Boolean messageSent;
	
	public Message(String chatID, String senderID, LocalDateTime timeStamp, String content) {
		this.chatID = chatID;
		this.senderID = senderID;
		this.timeStamp = timeStamp;
		this.content = content;
	}
	public Boolean checkStatus() {
		return messageSent;
	}
	
	public String getChatID() {
		return chatID;
	}
	public String getSenderID() {
		return senderID;
	}
	public LocalDateTime getTimeStamp() {
		return timeStamp;
	}
	public String getContent() {
		return content;
	}
	
	public String getMessageID() {
		return messageID;
	}
	public void setStatus(Boolean messageSent) {
		this.messageSent = messageSent;
	}

}
