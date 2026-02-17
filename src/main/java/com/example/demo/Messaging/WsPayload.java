package com.example.demo.Messaging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class WsPayload {

    // SEND_MESSAGE | GROUP_KEY
    private String type;

    private Long roomId;

    // encrypted message OR encrypted group key
    private String cipherText;
}
