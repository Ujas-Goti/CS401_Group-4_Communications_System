package server;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.util.List;

import common.Message;
import common.User;
import common.UserRole;

public class LoggerTest {

    private static final String TEST_LOG_FILE = "test_chat_log.txt";
    private static final String TEST_CREDENTIALS_FILE = "test_credentials_logger.txt";
    private Logger logWriter;

    @Before
    public void setUp() throws IOException {
        createTestCredentialsFile();
        logWriter = new Logger(TEST_LOG_FILE, TEST_CREDENTIALS_FILE);
        clearLogFile();
    }

    @After
    public void tearDown() {
        File logFile = new File(TEST_LOG_FILE);
        File credFile = new File(TEST_CREDENTIALS_FILE);
        if (logFile.exists()) {
            logFile.delete();
        }
        if (credFile.exists()) {
            credFile.delete();
        }
    }

    private void createTestCredentialsFile() throws IOException {
        try (java.io.FileWriter writer = new java.io.FileWriter(TEST_CREDENTIALS_FILE)) {
            writer.write("alice,pass1,GENERAL\n");
            writer.write("bob,pass2,ADMIN\n");
        }
    }

    private void clearLogFile() throws IOException {
        File logFile = new File(TEST_LOG_FILE);
        if (logFile.exists()) {
            logFile.delete();
        }
        logFile.createNewFile();
    }

    @Test
    public void singleMessageRoundTrip() throws IOException {
        Message msg = new Message("chat123", "sender1", "Hello world");
        logWriter.logMessage(msg);

        List<Message> history = logWriter.getMessagesForChat("chat123");
        assertEquals(1, history.size());
        assertEquals("Hello world", history.get(0).getContent());
        assertEquals("sender1", history.get(0).getSenderID());
    }

    @Test
    public void multipleChatsStaySeparated() throws IOException {
        Message first = new Message("chat123", "sender1", "Message 1");
        Message second = new Message("chat123", "sender2", "Message 2");
        Message third = new Message("chat456", "sender1", "Message 3");

        logWriter.logMessage(first);
        logWriter.logMessage(second);
        logWriter.logMessage(third);

        List<Message> chat123Messages = logWriter.getMessagesForChat("chat123");
        assertEquals(2, chat123Messages.size());

        List<Message> chat456Messages = logWriter.getMessagesForChat("chat456");
        assertEquals(1, chat456Messages.size());
    }

    @Test
    public void loggingSessionCreatesEntry() throws IOException {
        List<User> people = new java.util.ArrayList<>();
        people.add(new User("alice", "pass1", UserRole.GENERAL));
        people.add(new User("bob", "pass2", UserRole.ADMIN));

        ChatSession chat = new ChatSession(people, false, "Private Chat");
        logWriter.logSession(chat);

        List<String> sessions = logWriter.filterSessionsByUser("alice");
        assertFalse("expected at least one session", sessions.isEmpty());
    }

    @Test
    public void missingChatReturnsEmptyList() {
        assertTrue(logWriter.getMessagesForChat("nonexistent").isEmpty());
    }

    @Test
    public void loadUserByIdReadsCredentialsFile() {
        User alice = logWriter.loadUserByID("alice");
        assertNotNull(alice);
        assertEquals("alice", alice.getUsername());
        assertEquals("alice", alice.getUserID());
    }

    @Test
    public void loadUserByIdReturnsNullForUnknown() {
        assertNull(logWriter.loadUserByID("nonexistent"));
    }

    @Test
    public void readAllLogsReturnsFullText() throws IOException {
        Message msg1 = new Message("chat123", "sender1", "Message 1");
        Message msg2 = new Message("chat123", "sender2", "Message 2");

        logWriter.logMessage(msg1);
        logWriter.logMessage(msg2);

        String allLogs = logWriter.readAllLogs();
        assertNotNull(allLogs);
        assertTrue(allLogs.contains("Message 1"));
        assertTrue(allLogs.contains("Message 2"));
    }

    @Test
    public void filterSessionsByUserMatchesCount() throws IOException {
        List<User> firstPair = new java.util.ArrayList<>();
        firstPair.add(new User("alice", "pass1", UserRole.GENERAL));
        firstPair.add(new User("bob", "pass2", UserRole.ADMIN));

        List<User> secondPair = new java.util.ArrayList<>();
        secondPair.add(new User("alice", "pass1", UserRole.GENERAL));
        secondPair.add(new User("charlie", "pass3", UserRole.GENERAL));

        ChatSession first = new ChatSession(firstPair, false, "Chat 1");
        ChatSession second = new ChatSession(secondPair, false, "Chat 2");

        logWriter.logSession(first);
        logWriter.logSession(second);

        List<String> aliceSessions = logWriter.filterSessionsByUser("alice");
        assertEquals(2, aliceSessions.size());

        List<String> bobSessions = logWriter.filterSessionsByUser("bob");
        assertEquals(1, bobSessions.size());

        List<String> charlieSessions = logWriter.filterSessionsByUser("charlie");
        assertEquals(1, charlieSessions.size());
    }
}