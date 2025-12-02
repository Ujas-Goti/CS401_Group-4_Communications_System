## CS401 Group 4 – Communications System

A Java desktop client–server chat application with role-based access control, WhatsApp-themed GUI, and text-file logging.

---

### 1. Features

- **User authentication**
  - Login via `AuthGUI` using `Authentication` and a credentials text file.
  - Roles: **ADMIN** and **GENERAL** (`UserRole` enum).
- **Online presence**
  - Online/offline status via `OnlineStatus` enum.
  - Contact list highlights online users in green.
- **Private and group chat**
  - One-to-one and one-to-group `ChatSession`s.
  - Each user has a `ChatManager` that represents their list of conversations.
- **WhatsApp-themed GUI**
  - `AuthGUI` for login, `ClientGUI` for the main chat window.
  - Left: contacts & groups; Right: chats; Bottom: status bar.
- **Notifications**
  - `Notification` shows popup for new messages when a chat is not active.
- **Logging & persistence**
  - `Logger` logs sessions and messages to text files.
  - `ChatManager` can reconstruct chat history from logs.

---

### 2. Main Components

- **Client-side**
  - `AuthGUI`  
    - Login window (username/password).
    - Uses `Authentication` to validate credentials.
    - On success, launches `ClientGUI` for the logged-in `User`.
  - `ClientGUI`  
    - Main chat UI:
      - Contact list (`JList<User>`) with search/filter.
      - Hidden `JTabbedPane` of chats (one tab per conversation).
      - Buttons: **New Group**, **View Logs** (admins only), **Logout**.
    - Public methods matching class diagram:
      - `displayUserList`, `updateOnlineUsers`, `startPrivateChat`, `startGroupChat`,
        `openChatWindow`, `displayMessage`, `sendMessage`, `updateOwnStatus`,
        `viewChatLogsDialog`, `logout`, `isChatActive`, `displayAdminTools`.
  - `Notification`  
    - Small WhatsApp-themed popup for new messages.

- **Common models**
  - `User`  
    - Fields: `userID` (== username), `username`, `password`, `UserRole role`, `OnlineStatus status`.
  - `Message`  
    - Fields: `messageID`, `chatID`, `senderID`, `LocalDateTime timeStamp`, `content`, `messageSent`.
    - **Serializable** to be sent over `ObjectOutputStream`/`ObjectInputStream`.
  - Enums:
    - `UserRole` → `ADMIN`, `GENERAL`
    - `OnlineStatus` → `ONLINE`, `OFFLINE`

- **Server-side**
  - `ConnectionManager`  
    - Manages sockets and object streams:
      - `threadPool : ExecutorService`
      - `clientSockets : Map<String, Socket>`
      - `clientInput : Map<String, ObjectInputStream>`
      - `clientOutput : Map<String, ObjectOutputStream>`
    - Methods: `startManager`, `registerClient`, `disconnectClient`,
      `getOutputStream`, `getInputStream`.
  - `Authentication`  
    - Validates credentials from a text file (`username,password,role`).
    - Creates `User` objects and manages sessions.
  - `ChatSession`  
    - Represents a single conversation (private or group).
    - Fields: `chatID`, `chatName`, `List<User> participants`, `List<Message> messages`, `boolean isGroup`.
    - Methods: `addParticipant`, `removeParticipant`, `addMessage`, getters, `isGroupChat`.
  - `ChatManager`  
    - Manages all chat sessions for the system.
    - Maps users → sessions, routes messages, invokes `Logger`.
  - `Logger`  
    - Logs to a text file using two logical line types:
      - `SESSION|chatID|timestamp|participants|isGroup|chatName`
      - `MESSAGE|chatID|timestamp|senderID|content`
    - Supports loading chat IDs and sessions/messages for a user.

---

### 3. Data Files

- **Credentials file** (used by `Authentication`)
  - One file (e.g. `data/credentials.txt`)
  - Format per line:
    - `username,password,role`
  - Example:
    - `alice,alice123,GENERAL`
    - `bob,bob123,ADMIN`

- **Chat log file** (used by `Logger` / `ChatManager`)
  - One file (e.g. `data/chat_log.txt`)
  - Mixed lines:
    - Session:  
      `SESSION|chatID|2025-12-02T21:15:30|alice;bob|false|`
    - Message:  
      `MESSAGE|chatID|2025-12-02T21:17:10|alice|Hi Bob`

---

### 4. Message Flow (High Level)

1. **Client sends a message**
   - `ClientGUI.sendMessage(...)` creates a `Message`.
   - Client-side networking sends the `Message` object via `ObjectOutputStream` to the server.

2. **Server routes the message**
   - `ConnectionManager` reads the `Message` from the sender’s socket.
   - Server `ChatManager`:
     - Finds/creates the `ChatSession`.
     - Calls `session.addMessage(message)` and `logger.logMessage(message)`.
     - Determines participants and forwards the `Message` to online recipients via `ConnectionManager`.

3. **Recipients display it**
   - Their client receives the `Message` object.
   - Calls `ClientGUI.displayMessage(message)`:
     - Ensures a chat tab exists.
     - Appends `[HH:mm] senderID: content` to the text area.
     - Triggers `Notification` if the chat isn’t active.

Offline users later fetch messages from history (via `Logger`/`ChatManager` and the log file).

---

### 5. Compilation and Running (Local, no networking yet)

From project root (where `src` folder is):

# Compile
javac -d out src/common/*.java src/server/*.java src/client/*.java

# Example: run AuthGUI as entry point (once wired)
java -cp out client.AuthGUIDuring early development you can also run the standalone demo GUI:

javac -d out src/client/ClientGUITest.java
java -cp out client.ClientGUITest(If `ClientGUITest` is present; it’s optional and only for UI demo.)

---

### 6. Future Integration Steps

- Wire `AuthGUI` to create a socket connection and pass it (or a wrapper) into `ClientGUI`.
- Implement the server main class:
  - Create `ConnectionManager`, `Authentication`, `ChatManager`, `Logger`.
  - Accept client sockets, register them, and start listening for `Message` objects.
- Connect `ClientGUI.sendMessage(...)` and `viewChatLogsDialog()` to the real server calls.

---
