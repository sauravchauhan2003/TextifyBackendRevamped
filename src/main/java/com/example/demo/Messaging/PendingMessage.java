package com.example.demo.Messaging;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
public class PendingMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String receiverEmail;

    @ManyToOne
    private ChatRoom chatRoom;

    @Lob
    private String cipherText;

    private Instant createdAt;
}
