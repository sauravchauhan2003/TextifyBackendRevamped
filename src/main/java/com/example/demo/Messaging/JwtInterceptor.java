package com.example.demo.Messaging;

import com.example.demo.Authentication.JWTService;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;


import java.util.Map;
@Component
public class JwtInterceptor implements HandshakeInterceptor {
    @Autowired
    private JWTService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String token = extractToken(request);
        if(token.isEmpty()){
            return false;
        }
        try {
            String email = jwtService.extractEmail(token);

            // Store authenticated user info in session attributes
            attributes.put("user", email);

            System.out.println("✅ WebSocket authenticated for user: " + email);
            return true;

        } catch (Exception e) {
            System.out.println("❌ WebSocket rejected: Invalid JWT");
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, @Nullable Exception exception) {

    }
    private String extractToken(ServerHttpRequest request) {

        // 1️⃣ Authorization Header: Bearer <token>
        HttpHeaders headers = request.getHeaders();
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
