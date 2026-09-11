package com.ds.sensor.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DashboardWebSocketHandler.class);
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Dashboard WebSocket client connected: session {}", session.getId());
        sendDirect(session, Map.of(
            "type", "CONNECTED",
            "message", "Connected to Distributed System Aggregator Live Stream",
            "timestamp", System.currentTimeMillis()
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Dashboard WebSocket client disconnected: session {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Echo or handle client ping
        try {
            sendDirect(session, Map.of("type", "PONG", "received", message.getPayload()));
        } catch (Exception e) {
            log.warn("Error handling text message: {}", e.getMessage());
        }
    }

    public synchronized void broadcast(String type, Object data) {
        if (sessions.isEmpty()) return;

        Map<String, Object> envelope = new HashMap<>();
        envelope.put("type", type);
        envelope.put("data", data);
        envelope.put("timestamp", System.currentTimeMillis());

        try {
            String json = objectMapper.writeValueAsString(envelope);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error serializing WebSocket payload: {}", e.getMessage());
        }
    }

    private void sendDirect(WebSocketSession session, Object obj) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.warn("Failed to send direct message to session {}: {}", session.getId(), e.getMessage());
        }
    }
}
