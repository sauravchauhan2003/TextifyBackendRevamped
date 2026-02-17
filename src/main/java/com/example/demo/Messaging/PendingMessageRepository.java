package com.example.demo.Messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PendingMessageRepository
        extends JpaRepository<PendingMessage, Long> {

    List<PendingMessage> findByReceiverEmail(String email);

    void deleteByReceiverEmail(String email);
}
