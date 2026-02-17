package com.example.demo.Messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    // email → active session
    private static final ConcurrentHashMap<String, WebSocketSession>
            activeUsers = new ConcurrentHashMap<>();

    @Autowired
    private ChatRoomRepository roomRepo;

    @Autowired
    private ChatParticipantRepository participantRepo;

    @Autowired
    private PendingMessageRepository pendingRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {

        String email = (String) session.getAttributes().get("user");
        activeUsers.put(email, session);

        // 🔥 Deliver offline messages
        List<PendingMessage> pending =
                pendingRepo.findByReceiverEmail(email);

        for (PendingMessage msg : pending) {
            session.sendMessage(new TextMessage(msg.getCipherText()));
        }

        pendingRepo.deleteByReceiverEmail(email);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session,
                                     TextMessage message)
            throws Exception {

        WsPayload payload =
                mapper.readValue(message.getPayload(), WsPayload.class);

        if ("SEND_MESSAGE".equals(payload.getType())) {
            forwardMessage(payload);
        }

        if ("GROUP_KEY".equals(payload.getType())) {
            forwardGroupKey(payload);
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

    @Override
    public void afterConnectionClosed(WebSocketSession session,
                                      CloseStatus status) {

        String email = (String) session.getAttributes().get("user");
        activeUsers.remove(email);
    }
}
