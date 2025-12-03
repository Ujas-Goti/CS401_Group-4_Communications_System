package server;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import common.User;
import common.UserRole;
import common.OnlineStatus;

public class AuthenticationTest {

    private static final String TEST_CREDENTIALS_FILE = "test_credentials.txt";
    private Authentication auth;

    @Before
    public void setUp() throws IOException {
        createTestCredentialsFile();
        auth = new Authentication(TEST_CREDENTIALS_FILE);
    }

    @After
    public void tearDown() {
        File file = new File(TEST_CREDENTIALS_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    private void createTestCredentialsFile() throws IOException {
        try (FileWriter writer = new FileWriter(TEST_CREDENTIALS_FILE)) {
            writer.write("alice,password123,GENERAL\n");
            writer.write("bob,secret456,ADMIN\n");
            writer.write("charlie,pass789,GENERAL\n");
        }
    }

    @Test
    public void validUserBecomesOnline() {
        User alice = auth.validateCredentials("alice", "password123");

        assertNotNull(alice);
        assertEquals("alice", alice.getUsername());
        assertSame(UserRole.GENERAL, alice.getRole());
        assertSame(OnlineStatus.ONLINE, alice.getStatus());
    }

    @Test
    public void adminCredentialShowsAdminRole() {
        User bob = auth.validateCredentials("bob", "secret456");

        assertNotNull(bob);
        assertEquals("bob", bob.getUsername());
        assertSame(UserRole.ADMIN, bob.getRole());
    }

    @Test
    public void wrongPasswordIsRejected() {
        assertNull(auth.validateCredentials("alice", "wrongpassword"));
    }

    @Test
    public void unknownUserIsRejected() {
        assertNull(auth.validateCredentials("nonexistent", "password123"));
    }

    @Test
    public void nullUsernameIsRejected() {
        assertNull(auth.validateCredentials(null, "password123"));
    }

    @Test
    public void nullPasswordIsRejected() {
        assertNull(auth.validateCredentials("alice", null));
    }

    @Test
    public void emptyUsernameIsRejected() {
        assertNull(auth.validateCredentials("", "password123"));
    }

    @Test
    public void sessionCreationMarksUserOnline() {
        User alice = auth.validateCredentials("alice", "password123");
        assertNotNull(alice);

        String sessionId = auth.createSession(alice);
        assertNotNull(sessionId);
        assertTrue(auth.checkStatus(alice));
    }

    @Test
    public void secondSessionRequestIsRejected() {
        User alice = auth.validateCredentials("alice", "password123");
        assertNotNull(alice);

        String firstSession = auth.createSession(alice);
        assertNotNull(firstSession);

        assertNull(auth.createSession(alice));
    }

    @Test
    public void endingSessionClearsStatus() {
        User alice = auth.validateCredentials("alice", "password123");
        assertNotNull(alice);

        String sessionId = auth.createSession(alice);
        assertNotNull(sessionId);

        assertTrue(auth.endSession(sessionId));
        assertFalse(auth.checkStatus(alice));
    }

    @Test
    public void endingUnknownSessionFails() {
        assertFalse(auth.endSession("invalid123"));
    }

    @Test
    public void allRegisteredAccountsAreListed() {
        List<User> everyone = auth.getAllRegisteredUsers();

        assertEquals(3, everyone.size());
        assertTrue(everyone.stream().anyMatch(u -> "alice".equals(u.getUsername())));
        assertTrue(everyone.stream().anyMatch(u -> "bob".equals(u.getUsername())));
        assertTrue(everyone.stream().anyMatch(u -> "charlie".equals(u.getUsername())));
    }

    @Test
    public void sessionLookupByUserReturnsSession() {
        User alice = auth.validateCredentials("alice", "password123");
        assertNotNull(alice);

        String sessionId = auth.createSession(alice);
        assertNotNull(sessionId);

        UserSession savedSession = auth.getSessionFor(alice);
        assertNotNull(savedSession);
        assertEquals(alice.getUserID(), savedSession.getUser().getUserID());
    }

    @Test
    public void sessionLookupWithoutCreationReturnsNull() {
        User alice = auth.validateCredentials("alice", "password123");
        assertNotNull(alice);

        assertNull(auth.getSessionFor(alice));
    }
}