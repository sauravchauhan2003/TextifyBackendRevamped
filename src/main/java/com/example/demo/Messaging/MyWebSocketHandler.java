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
public class MyWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private DirectPendingRepository directPendingRepo;

    // email → active session
    private static final ConcurrentHashMap<String, WebSocketSession>
            activeUsers = new ConcurrentHashMap<>();

    @Autowired
    private ChatRoomRepository roomRepo;

    @Autowired
    private ChatParticipantRepository participantRepo;

    @Autowired
    private PendingMessageRepository pendingRepo;

    @Autowired
    private DirectMessageRepository directRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {

        String email = (String) session.getAttributes().get("user");
        activeUsers.put(email, session);

        // Deliver group offline messages
        List<PendingMessage> pending =
                pendingRepo.findByReceiverEmail(email);

        for (PendingMessage msg : pending) {
            session.sendMessage(new TextMessage(msg.getCipherText()));
        }

        pendingRepo.deleteByReceiverEmail(email);

        // Deliver direct offline messages
        List<DirectPendingMessage> directPending =
                directPendingRepo.findByReceiverEmail(email);

        for (DirectPendingMessage msg : directPending) {
            session.sendMessage(new TextMessage(msg.getCipherText()));
        }

        directPendingRepo.deleteByReceiverEmail(email);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session,
                                     TextMessage message)
            throws Exception {

        String raw = message.getPayload();

        // First parse minimal structure to check type
        BasePayload base =
                mapper.readValue(raw, BasePayload.class);

        if ("SEND_MESSAGE".equals(base.getType())) {

            WsPayload payload =
                    mapper.readValue(raw, WsPayload.class);

            forwardMessage(payload);
        }

        else if ("GROUP_KEY".equals(base.getType())) {

            WsPayload payload =
                    mapper.readValue(raw, WsPayload.class);

            forwardGroupKey(payload);
        }

        else if ("SEND_DIRECT".equals(base.getType())) {

            DirectWsPayload payload =
                    mapper.readValue(raw, DirectWsPayload.class);

            forwardDirectMessage(payload);
        }
    }

    private void forwardMessage(WsPayload payload) throws Exception {

        ChatRoom room =
                roomRepo.findById(payload.getRoomId()).orElseThrow();

        for (ChatParticipant p :
                participantRepo.findByChatRoom(room)) {

            String receiver = p.getUser().getEmail();
            WebSocketSession target = activeUsers.get(receiver);

            String json = mapper.writeValueAsString(payload);

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

    private void forwardGroupKey(WsPayload payload) throws Exception {

        ChatRoom room =
                roomRepo.findById(payload.getRoomId()).orElseThrow();

        for (ChatParticipant p :
                participantRepo.findByChatRoom(room)) {

            WebSocketSession target =
                    activeUsers.get(p.getUser().getEmail());

            if (target != null && target.isOpen()) {
                target.sendMessage(
                        new TextMessage(
                                mapper.writeValueAsString(payload)
                        )
                );
            }
        }
    }

    private void forwardDirectMessage(DirectWsPayload payload) throws Exception {

        String sender = payload.getSenderEmail();
        String receiver = payload.getReceiverEmail();

        String json = mapper.writeValueAsString(payload);

        // Save direct message history
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
            // Store as pending if offline
            DirectPendingMessage pending = new DirectPendingMessage();
            pending.setReceiverEmail(receiver);
            pending.setCipherText(json);
            pending.setCreatedAt(Instant.now());
            directPendingRepo.save(pending);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session,
                                      CloseStatus status) {

        String email = (String) session.getAttributes().get("user");
        activeUsers.remove(email);
    }
}