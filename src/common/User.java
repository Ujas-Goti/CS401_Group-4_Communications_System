
public class User {
	private String userID;
	private String username;
	private String password;
	private UserRole role;
	private OnlineStatus status = OnlineStatus.OFFLINE; // I think the default status for OnlineStatus should be OFFLINE. (Carlos)
	
	public User(String username, String password, UserRole role) {
		this.username = username;
		this.password = password;
		this.role = role;
	}
	
	public String getUserID() {
		return userID;
	}
	public String getUsername() {
		return username;
	}
	public UserRole getRole() {
		return role;
	}
	public OnlineStatus getStatus() {
		return status;
	}
	public void setStatus(OnlineStatus status) {
		this.status = status;
	}
	public void setUserName(String username) {
		this.username = username;
	}
	
	// Need to ask team what the format will be
	public toString() {
		return "";
	}
}
