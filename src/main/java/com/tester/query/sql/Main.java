package com.tester.query.sql;

import com.tester.query.sql.controller.RestController;
import com.tester.query.sql.service.RestService;
import com.tester.query.sql.websocket.WebSocketServer;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.RoutingHandler;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        String webAppPath = "C:/Users/Polina/Desktop/Diploma/Diploma project/SQLTest/src/main/webapp";

        // Static files (Frontend)
        ResourceHandler resourceHandler = new ResourceHandler(
                new FileResourceManager(new File(webAppPath), 1024))
                .setWelcomeFiles("index.html")
                .setDirectoryListingEnabled(false);

        // Initialize the Database Connection Factory
        RestService restService = new RestService();

        RoutingHandler routingHandler = Handlers.routing();
        new RestController(restService).registerRoutes(routingHandler);

        PathHandler pathHandler = Handlers.path()
                .addPrefixPath("/", resourceHandler)
                .addPrefixPath("/api", routingHandler);

        // Undertow start
        Undertow server = Undertow.builder()
                .addHttpListener(8080, "0.0.0.0")
                .setHandler(pathHandler)
                .build();

        server.start();
        System.out.println("Server is running on http://localhost:8080");

        // WebSocket start
        WebSocketServer.start();
    }
}
