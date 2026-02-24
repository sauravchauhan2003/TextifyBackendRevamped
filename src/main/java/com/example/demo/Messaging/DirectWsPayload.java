package com.example.demo.Messaging;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DirectWsPayload {

    private String type; // SEND_DIRECT

    private String senderEmail;

    private String receiverEmail;

    private String cipherText;
}