package com.example.demo.Messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgoraSignalingHandler extends TextWebSocketHandler {

    @Autowired
    private PendingNotificationRepository pendingNotificationRepo;

    private static final ConcurrentHashMap<String, WebSocketSession> activeUsers = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String email = (String) session.getAttributes().get("user");
        activeUsers.put(email, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String raw = message.getPayload();
        SignalingPayload payload = mapper.readValue(raw, SignalingPayload.class);
        
        String target = payload.getTargetEmail();
        WebSocketSession targetSession = activeUsers.get(target);

        if (targetSession != null && targetSession.isOpen()) {
            targetSession.sendMessage(new TextMessage(raw));
        } else {
            // Generate missed call notification if this is a call initiation
            if ("CALL".equals(payload.getType())) {
                PendingNotification notif = new PendingNotification();
                notif.setReceiverEmail(target);
                notif.setCreatedAt(Instant.now());
                
                String notifPayload = String.format("{\"type\":\"MISSED_CALL\", \"caller\":\"%s\", \"callType\":\"AGORA\"}", payload.getSenderEmail());
                notif.setPayload(notifPayload);
                
                pendingNotificationRepo.save(notif);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String email = (String) session.getAttributes().get("user");
        activeUsers.remove(email);
    }
}
