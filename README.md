# CS401 Group 4 – Communications System

A Java desktop client–server chat application with role-based access control, WhatsApp-themed GUI, and text-file logging.

## Features

- **User Authentication**: Login via credentials file with ADMIN/GENERAL roles
- **Online Presence**: Real-time online/offline status tracking
- **Private & Group Chat**: One-to-one and group conversations
- **WhatsApp-themed GUI**: Modern, user-friendly interface
- **Notifications**: Popup notifications for new messages
- **Logging & Persistence**: All sessions and messages logged to text files

## Project Structure

```
src/
├── common/          # Shared classes (User, Message, Enums)
├── server/          # Server-side code
│   ├── Server.java          # Main server with ClientHandler
│   ├── Authentication.java  # Credential validation
│   ├── ConnectionManager.java # Socket/stream management
│   ├── ChatManager.java     # Chat session management
│   ├── ChatSession.java     # Individual chat sessions
│   ├── Logger.java          # File logging
│   └── UserSession.java     # User session tracking
└── client/          # Client-side code
    ├── AuthGUI.java         # Login window (with main method)
    ├── ClientGUI.java        # Main chat interface
    ├── ClientConnection.java # Network client
    └── Notification.java     # Message notifications

data/
├── credentials.txt  # User credentials (username,password,role)
└── chat_log.txt     # Combined log file (messages and sessions)
```

## Setup Instructions

### 1. Compile the Project

From the project root directory:

```bash
# Create output directory
mkdir -p out

# Compile all Java files
javac -d out -cp . src/common/*.java src/server/*.java src/client/*.java
```

### 2. Prepare Data Files

Ensure the `data/` directory exists with:
- `credentials.txt`: Format `username,password,role` (one per line)
  - Example: `alice,alice123,GENERAL`
- `chat_log.txt`: Empty initially (auto-created)
- `sessions.txt`: Empty initially (auto-created)

### 3. Run the Server

On the **server machine**:

```bash
# Default port 1234
java -cp out server.Server

# Or specify a port
java -cp out server.Server 1234
```

The server will:
- Listen for client connections
- Authenticate users from `data/credentials.txt`
- Route messages between clients
- Log all sessions and messages to text files

### 4. Run the Client

On **each client machine**:

```bash
java -cp out client.AuthGUI
```

In the login window:
- Enter **username** and **password** (from credentials file)
- Enter **server host** (IP address or hostname of server machine)
- Enter **server port** (default: 1234)
- Click **Login**

## Usage

### For Users

1. **Login**: Enter credentials and server address
2. **View Contacts**: Left panel shows online users
3. **Start Chat**: Click a user to start a private chat
4. **Create Group**: Click "New Group", select users, enter group name
5. **Send Messages**: Type in the input field and click "Send"
6. **View History**: Previous messages load automatically when opening a chat

### For Admins

- Admins see a **"View Logs"** button (visible only to ADMIN role)
- All other features same as general users

## Network Configuration

- **Server**: Runs on one machine, listens on specified port (default 1234)
- **Clients**: Connect to server IP/port from their own machines
- **Protocol**: Uses Java ObjectInputStream/ObjectOutputStream for serialized Message objects

## Data File Formats

### credentials.txt
```
username,password,role
alice,alice123,GENERAL
bob,bob123,ADMIN
```

### chat_log.txt (auto-generated, combined file)
```
MESSAGE|messageID|chatID|senderID|timestamp|content
SESSION|chatID|timestamp|isGroup|participant1,participant2,...|chatName
```

## Troubleshooting

- **Connection refused**: Check server is running and firewall allows the port
- **Login failed**: Verify credentials in `data/credentials.txt`
- **User already logged in**: Only one session per user allowed
- **Messages not appearing**: Check server console for errors

## Development Notes

- `Message` class is `Serializable` for network transmission
- `User` and `ChatSession` are also `Serializable` for session data
- Server uses multi-threaded `ClientHandler` for each connected client
- Client uses `ClientConnection` with listener pattern for async message handling
- `ConnectionManager` creates and manages `ObjectInputStream`/`ObjectOutputStream` for each client
- All logs (messages and sessions) are stored in a single `chat_log.txt` file with `MESSAGE|` and `SESSION|` prefixes
