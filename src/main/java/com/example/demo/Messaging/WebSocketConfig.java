package com.example.demo.Messaging;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final MyWebSocketHandler myWebSocketHandler;  // 🔥 inject handler

    public WebSocketConfig(JwtInterceptor jwtInterceptor,
                           MyWebSocketHandler myWebSocketHandler) {
        this.jwtInterceptor = jwtInterceptor;
        this.myWebSocketHandler = myWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myWebSocketHandler, "/ws")   // 🔥 NO new()
                .addInterceptors(jwtInterceptor)
                .setAllowedOrigins("*");
    }
}
