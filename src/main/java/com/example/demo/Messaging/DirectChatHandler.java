package com.example.demo.Messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DirectChatHandler extends TextWebSocketHandler {

    @Autowired
    private DirectPendingRepository directPendingRepo;

    @Autowired
    private DirectMessageRepository directRepo;

    private static final ConcurrentHashMap<String, WebSocketSession> activeUsers = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String email = (String) session.getAttributes().get("user");
        activeUsers.put(email, session);

        List<DirectPendingMessage> pending = directPendingRepo.findByReceiverEmail(email);
        for (DirectPendingMessage msg : pending) {
            session.sendMessage(new TextMessage(msg.getCipherText()));
        }
        directPendingRepo.deleteByReceiverEmail(email);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String raw = message.getPayload();
        DirectWsPayload payload = mapper.readValue(raw, DirectWsPayload.class);
        
        String sender = payload.getSenderEmail();
        String receiver = payload.getReceiverEmail();
        String json = mapper.writeValueAsString(payload);

        DirectMessage dm = new DirectMessage();
        dm.setSenderEmail(sender);
        dm.setReceiverEmail(receiver);
        dm.setCipherText(payload.getCipherText());
        dm.setCreatedAt(Instant.now());
        directRepo.save(dm);

        WebSocketSession target = activeUsers.get(receiver);
        if (target != null && target.isOpen()) {
            target.sendMessage(new TextMessage(json));
        } else {
            DirectPendingMessage pending = new DirectPendingMessage();
            pending.setReceiverEmail(receiver);
            pending.setCipherText(json);
            pending.setCreatedAt(Instant.now());
            directPendingRepo.save(pending);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String email = (String) session.getAttributes().get("user");
        activeUsers.remove(email);
    }
}
