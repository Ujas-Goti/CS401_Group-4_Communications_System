package server;
import java.util.List;
import common.Message;

public class ServerAPI {
	private Logger logger;
	// Chat manager
		// Return logs
		// Update log
		// Chat routing:  clientGUI -> chatsession -> chatmanager ->  SERVER -> 
		//                chatmanager -> chatsession -> ClientGUI(recipient)
	
	public ServerAPI(){
		
	}
	
	public List<String> returnLog(){
		return logger.readAllSessions();
	}
	
	public List<String> returnFilteredLog(String userID){
		return logger.filterSessionsByUser(userID);
	}
	
	public void updateChatLog(Message message) {
		logger.logMessage(message);
	}

	
	// Authenticator
		// Authenticate credentials
		// Find role
		// Create User object for the person logging in
		// Return the user object back to the client
	
	// 
	
}
