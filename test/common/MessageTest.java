package common;

import static org.junit.Assert.*;
import org.junit.Test;
import java.time.LocalDateTime;

public class MessageTest {

    @Test
    public void messageCarriesSenderAndChat() {
        Message note = new Message("chat123", "sender1", "Hello world");

        assertNotNull(note.getMessageID());
        assertEquals("chat123", note.getChatID());
        assertEquals("sender1", note.getSenderID());
        assertEquals("Hello world", note.getContent());
        assertNotNull("timestamp should be assigned", note.getTimeStamp());
        assertTrue(note.checkStatus());
    }

    @Test
    public void generatedIdsAreUnique() {
        Message first = new Message("chat1", "sender1", "msg1");
        Message second = new Message("chat1", "sender1", "msg1");

        assertNotEquals(first.getMessageID(), second.getMessageID());
        assertNotSame(first, second);
    }

    @Test
    public void customTimestampIsHonored() {
        LocalDateTime moment = LocalDateTime.of(2024, 1, 1, 12, 0);
        Message snapshot = new Message("msgID", "chat123", "sender1", moment, "Content");

        assertEquals("msgID", snapshot.getMessageID());
        assertEquals("chat123", snapshot.getChatID());
        assertEquals(moment, snapshot.getTimeStamp());
        assertEquals("Content", snapshot.getContent());
    }

    @Test
    public void deliveryFlagToggles() {
        Message ping = new Message("chat123", "sender1", "Hello");
        assertTrue(ping.checkStatus());

        ping.setStatus(false);
        assertFalse(ping.checkStatus());

        ping.setStatus(true);
        assertTrue(ping.checkStatus());
    }

    @Test
    public void timestampLooksRecent() {
        LocalDateTime before = LocalDateTime.now();
        Message ping = new Message("chat123", "sender1", "Test");
        LocalDateTime after = LocalDateTime.now();

        assertTrue(ping.getTimeStamp().isAfter(before.minusSeconds(1)));
        assertTrue(ping.getTimeStamp().isBefore(after.plusSeconds(1)));
    }

    @Test
    public void emptyBodyIsAllowed() {
        Message blank = new Message("chat123", "sender1", "");
        assertEquals("", blank.getContent());
    }

    @Test
    public void longBodyIsPreserved() {
        String essay = "A".repeat(1000);
        Message longMsg = new Message("chat123", "sender1", essay);

        assertEquals(essay.length(), longMsg.getContent().length());
        assertEquals(essay, longMsg.getContent());
    }
}