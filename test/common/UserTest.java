package common;

import static org.junit.Assert.*;
import org.junit.Test;

public class UserTest {

    @Test
    public void userStartsOfflineWithGivenRole() {
        User newbie = new User("alice", "password123", UserRole.GENERAL);

        assertNotNull(newbie);
        assertEquals("alice", newbie.getUsername());
        assertSame(UserRole.GENERAL, newbie.getRole());
        assertSame(OnlineStatus.OFFLINE, newbie.getStatus());
    }

    @Test
    public void usernameMirrorsId() {
        User admin = new User("bob", "pass", UserRole.ADMIN);

        assertEquals(admin.getUsername(), admin.getUserID());
        assertSame(UserRole.ADMIN, admin.getRole());
    }

    @Test
    public void statusFlipsWhenToggled() {
        User member = new User("charlie", "pwd", UserRole.GENERAL);
        assertSame(OnlineStatus.OFFLINE, member.getStatus());

        member.setStatus(OnlineStatus.ONLINE);
        assertSame(OnlineStatus.ONLINE, member.getStatus());

        member.setStatus(OnlineStatus.OFFLINE);
        assertNotEquals(OnlineStatus.ONLINE, member.getStatus());
    }

    @Test
    public void customIdDoesNotChangeUsername() {
        User dev = new User("dave", "pass", UserRole.GENERAL);
        dev.setUserID("customID");

        assertEquals("customID", dev.getUserID());
        assertEquals("dave", dev.getUsername());
    }

    @Test
    public void renamingUserKeepsId() {
        User editor = new User("eve", "pass", UserRole.GENERAL);
        editor.setUserName("eveUpdated");

        assertEquals("eveUpdated", editor.getUsername());
        assertNotEquals(editor.getUsername(), editor.getUserID());
    }

    @Test
    public void equalityReliesOnUserId() {
        User aliceOne = new User("alice", "pass1", UserRole.GENERAL);
        User aliceTwo = new User("alice", "pass2", UserRole.ADMIN);
        User bob = new User("bob", "pass3", UserRole.GENERAL);

        assertTrue(aliceOne.equals(aliceTwo));
        assertFalse(aliceOne.equals(bob));
        assertTrue(aliceOne.equals(aliceOne));
    }

    @Test
    public void matchingIdsProduceSameHash() {
        User aliceOne = new User("alice", "pass1", UserRole.GENERAL);
        User aliceTwo = new User("alice", "pass2", UserRole.ADMIN);

        assertEquals(aliceOne.hashCode(), aliceTwo.hashCode());
    }

    @Test
    public void toStringReturnsUsername() {
        User verbal = new User("alice", "pass", UserRole.GENERAL);
        assertEquals("alice", verbal.toString());
    }

    @Test
    public void rolesStayDistinct() {
        User admin = new User("admin", "pass", UserRole.ADMIN);
        User general = new User("user", "pass", UserRole.GENERAL);

        assertSame(UserRole.ADMIN, admin.getRole());
        assertSame(UserRole.GENERAL, general.getRole());
        assertNotSame(admin.getRole(), general.getRole());
    }
}