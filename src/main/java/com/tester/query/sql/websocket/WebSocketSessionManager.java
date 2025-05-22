package com.tester.query.sql.websocket;

import io.undertow.websockets.core.WebSocketChannel;
import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WebSocketSessionManager {
    @Getter
    private static final Set<WebSocketChannel> sessions = Collections.synchronizedSet(new HashSet<>());

    public static void addSession(WebSocketChannel channel) {
        sessions.add(channel);
    }

    public static void broadcast(String message) {
        synchronized (sessions) {
            for (WebSocketChannel session : sessions) {
                WebSocketHandler.sendMessage(session, message);
            }
        }
    }
}
