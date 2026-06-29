package com.example.demo.Messaging;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final DirectChatHandler directChatHandler;
    private final GroupChatHandler groupChatHandler;
    private final DirectSignalingHandler directSignalingHandler;
    private final AgoraSignalingHandler agoraSignalingHandler;
    private final NotificationHandler notificationHandler;

    public WebSocketConfig(JwtInterceptor jwtInterceptor,
                           DirectChatHandler directChatHandler,
                           GroupChatHandler groupChatHandler,
                           DirectSignalingHandler directSignalingHandler,
                           AgoraSignalingHandler agoraSignalingHandler,
                           NotificationHandler notificationHandler) {
        this.jwtInterceptor = jwtInterceptor;
        this.directChatHandler = directChatHandler;
        this.groupChatHandler = groupChatHandler;
        this.directSignalingHandler = directSignalingHandler;
        this.agoraSignalingHandler = agoraSignalingHandler;
        this.notificationHandler = notificationHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(directChatHandler, "/ws/chat/direct")
                .addInterceptors(jwtInterceptor)
                .setAllowedOrigins("*");
                
        registry.addHandler(groupChatHandler, "/ws/chat/group")
                .addInterceptors(jwtInterceptor)
                .setAllowedOrigins("*");
                
        registry.addHandler(directSignalingHandler, "/ws/signaling/direct")
                .addInterceptors(jwtInterceptor)
                .setAllowedOrigins("*");
                
        registry.addHandler(agoraSignalingHandler, "/ws/signaling/agora")
                .addInterceptors(jwtInterceptor)
                .setAllowedOrigins("*");
                
        registry.addHandler(notificationHandler, "/ws/notifications")
                .addInterceptors(jwtInterceptor)
                .setAllowedOrigins("*");
    }
}
