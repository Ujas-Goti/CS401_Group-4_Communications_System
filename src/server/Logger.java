package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import common.Message;
import common.User;
import common.UserRole;

public class Logger {

    private final String logFile;
    private final String credentialsFile;

    public Logger(String logFile, String credentialsFile) {
        this.logFile = logFile;
        this.credentialsFile = credentialsFile;
    }

    // log a message in logFile with MESSAGE| prefix
    public synchronized void logMessage(Message message) {
        String line = String.format("MESSAGE|%s|%s|%s|%s|%s",
                message.getMessageID(),
                message.getChatID(),
                message.getSenderID(),
                message.getTimeStamp().toString(),
                message.getContent().replace("|", "/")
        );

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)))) {
            out.println(line);
        } catch (IOException e) {
            System.err.println("ERROR writing to log: " + e.getMessage());
        }
    }


    // read all messages for a chat (filter by MESSAGE| prefix)
    public synchronized List<Message> getMessagesForChat(String chatID) {
        List<Message> messages = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;

            while ((line = br.readLine()) != null) {
                if (!line.startsWith("MESSAGE|")) {
					continue;
				}

                String[] parts = line.substring(8).split("\\|");
                if ((parts.length < 5) || !parts[1].equals(chatID)) {
					continue;
				}

                String messageID = parts[0];
                String msgchatID = parts[1];
                String senderID = parts[2];
                LocalDateTime timestamp = LocalDateTime.parse(parts[3]);
                String content = parts[4].replace("/", "|");

                Message msg = new Message(messageID, msgchatID, senderID, timestamp, content);
                messages.add(msg);
            }
        } catch (IOException e) {
            System.err.println("ERROR reading log: " + e.getMessage());
        }
        return messages;
    }

    // log a chat session in logFile with SESSION| prefix
    public synchronized void logSession(ChatSession session) {
        String participants = session.getParticipants().stream().map(User::getUserID).collect(Collectors.joining(","));
        String chatName = session.getChatName() != null ? session.getChatName() : "";
        String line = String.format("SESSION|%s|%s|%b|%s|%s",
                session.getChatID(),
                LocalDateTime.now().toString(),
                session.isGroup(),
                participants,
                chatName);

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)))) {
            out.println(line);
        } catch (IOException e) {
            System.err.println("ERROR writing session log: " + e.getMessage());
        }
    }

    // read all chat sessions from file (filter by SESSION| prefix)
    public synchronized List<String> readAllSessions() {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("SESSION|")) {
                    lines.add(line.substring(8));
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR reading sessions log: " + e.getMessage());
        }
        return lines;
    }


    // get sessions containing a user
    public synchronized List<String> filterSessionsByUser(String userID) {
        return readAllSessions().stream().filter(line -> {
            String[] parts = line.split("\\|");
            if (parts.length < 4) {
				return false;
			}
            String[] users = parts[3].split(",");
            for (String u : users) {
				if (u.equals(userID)) {
					return true;
				}
			}
            return false;
        }).collect(Collectors.toList());
    }

    // load user by ID from credentials file
    public synchronized User loadUserByID(String userID) {
        try (BufferedReader br = new BufferedReader(new FileReader(credentialsFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 3) {
					continue;
				}
                if (parts[0].equals(userID)) {
                    String username = parts[0];
                    String password = parts[1];
                    UserRole role = UserRole.valueOf(parts[2].toUpperCase());

                    return new User(username, password, role);		// now updated to match new main User constructor (username, password, role)
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR loading user: " + e.getMessage());
        }
        return null;
    }

    // Read entire log file contents (for admin viewing)
    public synchronized String readAllLogs() {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("ERROR reading log file: " + e.getMessage());
            return "Error reading log file: " + e.getMessage();
        }
        return content.toString();
    }
}
