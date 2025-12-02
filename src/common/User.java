import java.util.Objects;
import java.io.Serializable;

public class User implements Serializable {
	private String userID;
	private String username;
	private String password;
	private UserRole role;
	private OnlineStatus status = OnlineStatus.OFFLINE; // I think the default status for OnlineStatus should be OFFLINE. (Carlos O)
	
	public User(String username, String password, UserRole role) {
		this.username = username;
		this.password = password;
		this.role = role;
		this.userID = username;
	}
	
	public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
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

    @Override
    public String toString() {
        return username;
       
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return this.userID.equals(other.userID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID);
    }
    
}
