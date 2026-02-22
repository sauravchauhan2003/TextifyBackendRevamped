package com.example.demo.Authentication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private boolean publicKeyUploaded;
    private String Publickey;
}
