package com.connecthub.websocket.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Intercepts STOMP CONNECT frames to validate the JWT token.
 *
 * HOW WEBSOCKET AUTH WORKS:
 * 1. Client sends CONNECT frame with header: Authorization: Bearer <token>
 * 2. This interceptor extracts and validates the token
 * 3. If valid, it sets the authenticated user in the STOMP session
 * 4. If invalid, the connection is rejected
 *
 * WHY not use HTTP security for WebSocket auth?
 *   The HTTP handshake happens once and upgrades to WebSocket.
 *   After that, STOMP frames carry the auth token. We need to
 *   validate at the STOMP level, not the HTTP level.
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketAuthInterceptor implements WebSocketMessageBrokerConfigurer {

    @Value("${jwt.secret:connecthubsecretkey123456789abcdefghijklmnopqrstuvwxyz}")
    private String jwtSecret;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // Only intercept CONNECT frames (initial connection)
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        try {
                            String token = authHeader.substring(7);
                            SecretKey key = Keys.hmacShaKeyFor(
                                jwtSecret.getBytes(StandardCharsets.UTF_8));

                            io.jsonwebtoken.Claims claims = Jwts.parser().verifyWith(key).build()
                                    .parseSignedClaims(token).getPayload();
                            String userId = claims.getSubject();
                            String role = (String) claims.get("role");

                            java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                            if (role != null) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                            }

                            // Set the authenticated user for this STOMP session
                            UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userId, token, authorities);
                            accessor.setUser(auth);

                        } catch (Exception e) {
                            // Invalid token — connection will fail
                            throw new IllegalArgumentException("Invalid JWT token in WebSocket CONNECT");
                        }
                    }
                }
                return message;
            }
        });
    }
}
