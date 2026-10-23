package com.cmsc13.frontend;

import com.cmsc13.model.GameSession;

/** Shared state handed between screens (the session currently being played). */
public final class AppState {

    private static GameSession session;

    private AppState() {
    }

    public static GameSession getSession() {
        return session;
    }

    public static void setSession(GameSession s) {
        session = s;
    }
}
