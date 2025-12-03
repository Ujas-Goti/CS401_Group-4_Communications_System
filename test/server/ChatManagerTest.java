package server;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import common.User;
import common.UserRole;
import common.Message;
import common.OnlineStatus;

public class ChatManagerTest {

    private ChatManager manager;
    private Logger log;
    private User alice;
    private User bob;
    private User charlie;

    @Before
    public void setUp() {
        log = new Logger("test_log.txt", "test_credentials.txt");
        manager = new ChatManager(log);

        alice = new User("alice", "pass1", UserRole.GENERAL);
        alice.setStatus(OnlineStatus.ONLINE);
        bob = new User("bob", "pass2", UserRole.ADMIN);
        bob.setStatus(OnlineStatus.ONLINE);
        charlie = new User("charlie", "pass3", UserRole.GENERAL);
        charlie.setStatus(OnlineStatus.ONLINE);
    }

    @Test
    public void privateSessionRegistersParticipants() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = manager.createSession(people, false, "Private");

        assertNotNull(chat);
        assertNotNull(chat.getChatID());
        assertEquals(2, chat.getParticipants().size());
        assertFalse(chat.isGroupChat());
    }

    @Test
    public void groupSessionStoresName() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);
        people.add(charlie);

        ChatSession room = manager.createSession(people, true, "Test Group");

        assertNotNull(room);
        assertTrue(room.isGroupChat());
        assertEquals("Test Group", room.getChatName());
        assertEquals(3, room.getParticipants().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyParticipantListIsRejected() {
        manager.createSession(new ArrayList<>(), false, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullParticipantListIsRejected() {
        manager.createSession(null, false, "");
    }

    @Test
    public void joiningSessionAllowsReceivingMessages() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = manager.createSession(people, false, "");
        manager.joinSession(chat.getChatID(), alice);

        Message ping = new Message(chat.getChatID(), bob.getUserID(), "Test");
        List<User> targets = manager.receiveMessage(ping);
        assertTrue(targets.contains(alice));
    }

    @Test
    public void nullJoinRequestsAreIgnored() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = manager.createSession(people, false, "");
        manager.joinSession(null, alice);
        manager.joinSession(chat.getChatID(), null);
    }

    @Test
    public void leavingSessionStopsDelivery() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = manager.createSession(people, false, "");
        manager.joinSession(chat.getChatID(), alice);
        manager.leaveSession(chat.getChatID(), alice);

        Message ping = new Message(chat.getChatID(), bob.getUserID(), "Test");
        List<User> targets = manager.receiveMessage(ping);
        assertFalse(targets.contains(alice));
    }

    @Test
    public void directMessageTargetsOtherParticipant() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = manager.createSession(people, false, "");
        Message hello = new Message(chat.getChatID(), alice.getUserID(), "Hello");

        List<User> targets = manager.receiveMessage(hello);

        assertEquals(1, chat.getMessages().size());
        assertTrue(targets.contains(bob));
        assertFalse(targets.contains(alice));
    }

    @Test
    public void groupMessageHitsOtherMembers() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);
        people.add(charlie);

        ChatSession chat = manager.createSession(people, true, "Group");
        Message hello = new Message(chat.getChatID(), alice.getUserID(), "Hello group");

        List<User> targets = manager.receiveMessage(hello);

        assertEquals(1, chat.getMessages().size());
        assertTrue(targets.contains(bob));
        assertTrue(targets.contains(charlie));
        assertFalse(targets.contains(alice));
    }

    @Test
    public void nullMessagesReturnEmptyTargets() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        manager.createSession(people, false, "");
        List<User> targets = manager.receiveMessage(null);

        assertTrue(targets.isEmpty());
    }

    @Test
    public void unknownSessionReturnsEmptyTargets() {
        Message ghost = new Message("nonexistent", alice.getUserID(), "Hello");
        List<User> targets = manager.receiveMessage(ghost);

        assertTrue(targets.isEmpty());
    }

    @Test
    public void chatLookupReturnsSavedSession() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession created = manager.createSession(people, false, "");
        ChatSession found = manager.getChatSession(created.getChatID());

        assertNotNull(found);
        assertEquals(created.getChatID(), found.getChatID());
    }

    @Test
    public void chatLookupForUnknownIdReturnsNull() {
        assertNull(manager.getChatSession("nonexistent"));
    }

    @Test
    public void multipleSessionsCoexist() {
        List<User> roomOne = new ArrayList<>();
        roomOne.add(alice);
        roomOne.add(bob);

        List<User> roomTwo = new ArrayList<>();
        roomTwo.add(bob);
        roomTwo.add(charlie);

        ChatSession first = manager.createSession(roomOne, false, "Chat1");
        ChatSession second = manager.createSession(roomTwo, false, "Chat2");

        assertNotEquals(first.getChatID(), second.getChatID());
        assertEquals(first, manager.getChatSession(first.getChatID()));
        assertEquals(second, manager.getChatSession(second.getChatID()));
    }

    @Test
    public void findPrivateSessionMatchesByIds() {
        List<User> pair = new ArrayList<>();
        pair.add(alice);
        pair.add(bob);

        ChatSession session = manager.createSession(pair, false, "");
        ChatSession found = manager.findExistingPrivateSession(pair);

        assertNotNull(found);
        assertEquals(session.getChatID(), found.getChatID());
    }

    @Test
    public void findPrivateSessionIgnoresOrder() {
        List<User> forward = new ArrayList<>();
        forward.add(alice);
        forward.add(bob);

        List<User> reverse = new ArrayList<>();
        reverse.add(bob);
        reverse.add(alice);

        ChatSession session = manager.createSession(forward, false, "");
        ChatSession found = manager.findExistingPrivateSession(reverse);

        assertNotNull(found);
        assertEquals(session.getChatID(), found.getChatID());
    }

    @Test
    public void findPrivateSessionReturnsNullWhenMissing() {
        List<User> pair = new ArrayList<>();
        pair.add(alice);
        pair.add(charlie);

        assertNull(manager.findExistingPrivateSession(pair));
    }
}