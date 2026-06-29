package com.example.demo.Messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PendingNotificationRepository extends JpaRepository<PendingNotification, Long> {
    List<PendingNotification> findByReceiverEmail(String receiverEmail);
    
    @Transactional
    void deleteByReceiverEmail(String receiverEmail);
}
