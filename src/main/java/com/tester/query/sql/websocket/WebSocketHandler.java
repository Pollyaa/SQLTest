package com.tester.query.sql.websocket;

import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.core.WebSocketChannel;
import org.json.JSONObject;
import com.tester.query.sql.testing.DeleteBenchmark;
import com.tester.query.sql.testing.InsertBenchmark;
import com.tester.query.sql.testing.SelectBenchmark;
import com.tester.query.sql.testing.SelectJoinBenchmark;
import com.tester.query.sql.testing.UpdateBenchmark;
import com.tester.query.sql.testing.StressTesting;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketHandler extends AbstractReceiveListener {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
        String data = message.getData();
        try {
            JSONObject json = new JSONObject(data);
            String action = json.optString("action");
            if (action.equals("start-test")) {
                String queryType = json.optString("queryType");
                handleTestStart(channel, queryType);
            } else {
                sendError(channel, "Unknown action: " + action);
            }
        } catch (Exception e) {
            sendError(channel, "Error message handling: " + e.getMessage());
        }
    }

    private void handleTestStart(WebSocketChannel channel, String queryType) {
        executor.submit(() -> {
            try {
                switch (queryType) {
                    case "INSERT" -> new InsertBenchmark().runTesting();
                    case "SELECT" -> new SelectBenchmark().runTesting();
                    case "SELECT_JOIN" -> new SelectJoinBenchmark().runTesting();
                    case "UPDATE" -> new UpdateBenchmark().runTesting();
                    case "DELETE" -> new DeleteBenchmark().runTesting();
                    case "STRESS" -> new StressTesting().runTesting();
                    default -> sendError(channel, "Unknown query type: " + queryType);
                }
            } catch (SQLException e) {
                sendError(channel, "Error while running test: " + e.getMessage());
            }
        });
    }

    private void sendError(WebSocketChannel channel, String errorMessage) {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("success", false);
        errorResponse.put("error", errorMessage);
        sendMessage(channel, errorResponse.toString());
    }

    public static void sendMessage(WebSocketChannel channel, String message) {
        WebSockets.sendText(message, channel, null);
    }
}
