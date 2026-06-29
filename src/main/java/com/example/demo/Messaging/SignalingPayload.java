package com.example.demo.Messaging;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignalingPayload {
    private String type; // OFFER, ANSWER, ICE, CALL, RING
    private String senderEmail;
    private String targetEmail;
    private String data;
}
