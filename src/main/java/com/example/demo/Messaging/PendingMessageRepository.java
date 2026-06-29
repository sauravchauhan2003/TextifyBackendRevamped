package com.example.demo.Messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PendingMessageRepository
        extends JpaRepository<PendingMessage, Long> {

    List<PendingMessage> findByReceiverEmail(String receiverEmail);

    @Transactional
    void deleteByReceiverEmail(String receiverEmail);
}
