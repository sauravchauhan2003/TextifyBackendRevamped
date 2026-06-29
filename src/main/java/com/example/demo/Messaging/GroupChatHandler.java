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
public class GroupChatHandler extends TextWebSocketHandler {

    @Autowired
    private PendingMessageRepository pendingRepo;

    @Autowired
    private ChatRoomRepository roomRepo;

    @Autowired
    private ChatParticipantRepository participantRepo;

    private static final ConcurrentHashMap<String, WebSocketSession> activeUsers = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String email = (String) session.getAttributes().get("user");
        activeUsers.put(email, session);

        List<PendingMessage> pending = pendingRepo.findByReceiverEmail(email);
        for (PendingMessage msg : pending) {
            session.sendMessage(new TextMessage(msg.getCipherText()));
        }
        pendingRepo.deleteByReceiverEmail(email);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String raw = message.getPayload();
        WsPayload payload = mapper.readValue(raw, WsPayload.class);

        ChatRoom room = roomRepo.findById(payload.getRoomId()).orElseThrow();
        String json = mapper.writeValueAsString(payload);

        for (ChatParticipant p : participantRepo.findByChatRoom(room)) {
            String receiver = p.getUser().getEmail();
            WebSocketSession target = activeUsers.get(receiver);

            if (target != null && target.isOpen()) {
                target.sendMessage(new TextMessage(json));
            } else {
                PendingMessage pm = new PendingMessage();
                pm.setReceiverEmail(receiver);
                pm.setChatRoom(room);
                pm.setCipherText(json);
                pm.setCreatedAt(Instant.now());
                pendingRepo.save(pm);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String email = (String) session.getAttributes().get("user");
        activeUsers.remove(email);
    }
}
