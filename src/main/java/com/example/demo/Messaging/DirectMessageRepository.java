package com.example.demo.Messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    List<DirectMessage> findBySenderEmailAndReceiverEmailOrderByCreatedAtAsc(
            String senderEmail,
            String receiverEmail
    );

    List<DirectMessage> findByReceiverEmailOrderByCreatedAtAsc(
            String receiverEmail
    );

    // Custom method implemented manually
    default void savePending(DirectPendingMessage pending) {
        throw new UnsupportedOperationException("Implement using DirectPendingRepository");
    }
}