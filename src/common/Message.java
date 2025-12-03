package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
	private final String messageID;
	private final String chatID;
	private final String senderID;
	private final LocalDateTime timeStamp;
	private final String content;
	private Boolean messageSent;


	private String generateMessageID() {
		return java.util.UUID.randomUUID().toString();
	}


	public Message(String chatID, String senderID, String content) {
		this.messageID = generateMessageID();
		this.chatID = chatID;
		this.senderID = senderID;
		this.timeStamp = LocalDateTime.now();
		this.content = content;
		this.messageSent = true;
	}

	//	Constructor for Looger to rebuild messages from log
	public Message(String messageID, String chatID, String senderID, LocalDateTime timeStamp, String content) {
		this.messageID = messageID;
		this.chatID = chatID;
		this.senderID = senderID;
		this.timeStamp = timeStamp;
		this.content = content;
		//this.messageSent = messageSent;

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
