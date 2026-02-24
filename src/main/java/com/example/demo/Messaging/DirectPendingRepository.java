package com.example.demo.Messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectPendingRepository extends JpaRepository<DirectPendingMessage, Long> {

    List<DirectPendingMessage> findByReceiverEmail(String receiverEmail);

    void deleteByReceiverEmail(String receiverEmail);
}