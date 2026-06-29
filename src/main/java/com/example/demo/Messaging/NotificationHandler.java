package com.example.demo.Messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationHandler extends TextWebSocketHandler {

    @Autowired
    private PendingNotificationRepository pendingNotificationRepo;

    private static final ConcurrentHashMap<String, WebSocketSession> activeUsers = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String email = (String) session.getAttributes().get("user");
        activeUsers.put(email, session);

        List<PendingNotification> pending = pendingNotificationRepo.findByReceiverEmail(email);
        for (PendingNotification notif : pending) {
            session.sendMessage(new TextMessage(notif.getPayload()));
        }
        pendingNotificationRepo.deleteByReceiverEmail(email);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Notifications are primarily one-way (server to client)
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String email = (String) session.getAttributes().get("user");
        activeUsers.remove(email);
    }
}
