package com.tester.query.sql.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tester.query.sql.model.ConnectionRequest;
import com.tester.query.sql.service.RestService;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.Headers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RestController {
    private final RestService restService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RestController(RestService restService) {
        this.restService = restService;
    }

    public void registerRoutes(RoutingHandler router) {
        router.post("/connect", new BlockingHandler(this::handleConnect));
        router.get("/status", new BlockingHandler(this::handleStatus));
        router.get("/structure", new BlockingHandler(this::handleGetStructure));
        router.post("/disconnect", new BlockingHandler(this::handleDisconnect));
    }

    private void handleConnect(HttpServerExchange exchange) throws IOException {
        exchange.startBlocking();
        ConnectionRequest request = objectMapper.readValue(exchange.getInputStream(), ConnectionRequest.class);
        try {
            restService.connect(request);
            exchange.setStatusCode(200);
            exchange.getResponseSender().send("Підключення успішне!");
        } catch (Exception e) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("Помилка підключення: " + e.getMessage());
        }
    }

    private void handleStatus(HttpServerExchange exchange) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("connected", restService.isConnected());

        if (restService.isConnected()) {
            response.put("status", "Підключення активне");
            response.put("details", restService.getConnectionDetails());
        } else {
            response.put("status", "Немає підключення");
        }

        exchange.setStatusCode(200);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(objectMapper.writeValueAsString(response));
    }

    private void handleGetStructure(HttpServerExchange exchange) {
        if (!restService.isConnected()) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("Немає активного підключення до бази даних.");
            return;
        }

        try {
            Map<String, Object> structure = restService.getDatabaseStructure();
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(objectMapper.writeValueAsString(structure));
        } catch (Exception e) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("Помилка отримання структури: " + e.getMessage());
        }
    }

    private void handleDisconnect(HttpServerExchange exchange) {
        Map<String, Object> response = new HashMap<>();
        try {
            restService.disconnect();
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            exchange.setStatusCode(500);
        }

        try {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(objectMapper.writeValueAsString(response));
        } catch (IOException e) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("{\"success\": false, \"error\": \"Помилка формування відповіді\"}");
        }
    }
}
