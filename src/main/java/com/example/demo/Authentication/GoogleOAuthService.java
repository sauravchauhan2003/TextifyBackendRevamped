package com.example.demo.Authentication;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleOAuthService {

    private static final String CLIENT_ID =
            "979112656426-o7p5egc5o1d39s5dpt4levh55p0mgqn0.apps.googleusercontent.com";

    public GoogleIdToken.Payload verify(String idToken) {
        try {
            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(
                            new NetHttpTransport(),
                            new JacksonFactory())
                            .setAudience(Collections.singletonList(CLIENT_ID))
                            .build();

            GoogleIdToken token = verifier.verify(idToken);

            if (token == null) {
                throw new RuntimeException("Invalid Google ID Token");
            }

            return token.getPayload();

        } catch (Exception e) {
            throw new RuntimeException("Google token verification failed");
        }
    }
}
