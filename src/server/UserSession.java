package server;

import java.util.concurrent.atomic.AtomicInteger;

import common.User;


public class UserSession {

    private static final AtomicInteger NEXT_SESSION_ID = new AtomicInteger(1);

    private final int sessionID;
    private final User user;

    public UserSession(User user) {
        this.user = user;
        this.sessionID = NEXT_SESSION_ID.getAndIncrement();
    }

    public int getSessionID() {
        return sessionID;
    }

    public User getUser() {
        return user;
    }
}

