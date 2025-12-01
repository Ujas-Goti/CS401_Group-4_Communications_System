
import java.io.*;
import java.time.LocalDateTime;

public class Logger {

    private final String logFile;

    public Logger(String logFile) {
        this.logFile = logFile;
    }

    //	Saves a message to the log file in the format: messageID|chatSessionID|senderUserID|timestamp|messageText
    public synchronized void logMessage(Message message) {
        String line = String.format("%s|%s|%s|%s|%s",
                message.getMessageID(),
                message.getChatSessionID(),
                message.getSenderUserID(),
                LocalDateTime.now(),
                message.getText().replace("|", "/") 	// replace to avoid breaking format
        );

        
        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(line);

        } catch (IOException e) {
            System.err.println("ERROR writing to log: " + e.getMessage());
        }
    }

    
    //	Returns the entire log as a List<String>
    public synchronized List<String> readAll() {
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("ERROR reading log: " + e.getMessage());
        }

        return lines;
    }

    
    //	returns lines filtered by chat session ID
    public synchronized List<String> filterByChat(String chatID) {
        return readAll()
                .stream()
                .filter(line -> line.split("\\|")[1].equals(chatID))
                .toList();
    }

    //	returns lines filtered by chat user ID
    public synchronized List<String> filterByUser(String userID) {
        return readAll()
                .stream()
                .filter(line -> line.split("\\|")[2].equals(userID))
                .toList();
    }
}
