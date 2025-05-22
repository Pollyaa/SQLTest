package com.tester.query.sql.websocket;

import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;

public class WebSocketServer {
    public static void start() {
        RoutingHandler wsRoutingHandler = new RoutingHandler();

        wsRoutingHandler.get("/ws", new WebSocketProtocolHandshakeHandler(
                (WebSocketConnectionCallback) (exchange, channel) -> {
                    System.out.println("Підключено нову сесію: " + channel.getSourceAddress());
                    WebSocketSessionManager.addSession(channel);
                    channel.getReceiveSetter().set(new WebSocketHandler());
                    channel.resumeReceives();
                }));

        PathHandler pathHandler = new PathHandler().addPrefixPath("/", wsRoutingHandler);

        Undertow server = Undertow.builder()
                .addHttpListener(8081, "localhost")
                .setHandler(pathHandler)
                .build();

        server.start();
        System.out.println("🚀 WebSocket-сервер запущено на ws://localhost:8081/ws");
    }
}
