package com.example.demo.Messaging;

import com.example.demo.Authentication.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository
        extends JpaRepository<ChatParticipant, Long> {

    List<ChatParticipant> findByChatRoom(ChatRoom room);
    Optional<ChatParticipant> findByChatRoomAndUser(ChatRoom room, UserModel user);
    
    @Transactional
    void deleteByChatRoom(ChatRoom room);
}
