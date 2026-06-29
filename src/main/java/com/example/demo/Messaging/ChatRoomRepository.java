package com.example.demo.Messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ChatRoomRepository
        extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByInviteLink(String inviteLink);
}
