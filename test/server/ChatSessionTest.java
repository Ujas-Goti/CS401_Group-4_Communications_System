package server;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import common.User;
import common.UserRole;
import common.Message;

public class ChatSessionTest {

    private User alice;
    private User bob;
    private User charlie;

    @Before
    public void setUp() {
        alice = new User("alice", "pass1", UserRole.GENERAL);
        bob = new User("bob", "pass2", UserRole.ADMIN);
        charlie = new User("charlie", "pass3", UserRole.GENERAL);
    }

    @Test
    public void privateChatStartsAsNonGroup() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = new ChatSession(people, false, "");

        assertNotNull(chat.getChatID());
        assertFalse(chat.isGroupChat());
        assertEquals(2, chat.getParticipants().size());
        assertTrue(chat.getMessages().isEmpty());
    }

    @Test
    public void groupChatKeepsItsName() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);
        people.add(charlie);

        ChatSession squad = new ChatSession(people, true, "Test Group");

        assertNotNull(squad.getChatID());
        assertTrue(squad.isGroupChat());
        assertEquals("Test Group", squad.getChatName());
        assertEquals(3, squad.getParticipants().size());
    }

    @Test
    public void addingMessageStoresContent() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = new ChatSession(people, false, "");
        Message hello = new Message(chat.getChatID(), alice.getUserID(), "Hello");

        chat.addMessage(hello);
        assertEquals(1, chat.getMessages().size());
        assertEquals("Hello", chat.getMessages().get(0).getContent());
    }

    @Test
    public void addingNullMessageDoesNothing() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = new ChatSession(people, false, "");
        chat.addMessage(null);
        assertTrue(chat.getMessages().isEmpty());
    }

    @Test
    public void addingParticipantTurnsChatIntoGroup() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = new ChatSession(people, false, "");
        chat.addParticipant(charlie);

        assertEquals(3, chat.getParticipants().size());
        assertTrue(chat.isGroupChat());
    }

    @Test
    public void duplicateParticipantIsIgnored() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = new ChatSession(people, false, "");
        chat.addParticipant(alice);

        assertEquals(2, chat.getParticipants().size());
    }

    @Test
    public void nullParticipantIsIgnored() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = new ChatSession(people, false, "");
        chat.addParticipant(null);

        assertEquals(2, chat.getParticipants().size());
    }

    @Test
    public void removingMemberShrinksGroup() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);
        people.add(charlie);

        ChatSession chat = new ChatSession(people, true, "Group");
        chat.removeParticipant(charlie);

        assertEquals(2, chat.getParticipants().size());
        assertFalse(chat.isGroupChat());
    }

    @Test
    public void removingAnotherMemberAlsoShrinksGroup() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);
        people.add(charlie);

        ChatSession chat = new ChatSession(people, true, "Group");
        chat.removeParticipant(alice);

        assertEquals(2, chat.getParticipants().size());
        assertFalse(chat.isGroupChat());
    }

    @Test
    public void chatIdsAreUnique() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession first = new ChatSession(people, false, "");
        ChatSession second = new ChatSession(people, false, "");

        assertNotEquals(first.getChatID(), second.getChatID());
    }

    @Test
    public void constructorWithIdRespectsValue() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        String customId = "custom-chat-id";
        ChatSession chat = new ChatSession(customId, people, false, "Private");

        assertEquals(customId, chat.getChatID());
        assertEquals("Private", chat.getChatName());
    }

    @Test
    public void nullNameFallsBackToEmpty() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = new ChatSession(people, false, null);
        assertEquals("", chat.getChatName());
    }

    @Test
    public void messageListIsDefensiveCopy() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = new ChatSession(people, false, "");
        List<Message> external = chat.getMessages();
        external.add(new Message("id", "chat", "sender", "msg"));

        assertTrue(chat.getMessages().isEmpty());
    }

    @Test
    public void participantListIsDefensiveCopy() {
        List<User> people = new ArrayList<>();
        people.add(alice);
        people.add(bob);

        ChatSession chat = new ChatSession(people, false, "");
        List<User> outside = chat.getParticipants();
        outside.add(charlie);

        assertEquals(2, chat.getParticipants().size());
    }
}