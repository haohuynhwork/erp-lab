package com.erplab.realtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.erplab.common.config.AppProperties;

/**
 * STOMP over WebSocket at {@code <api prefix>/ws}. The handshake is open (see {@code SecurityConfig}); the
 * bearer token is checked on the STOMP CONNECT frame. Clients only subscribe to {@link #TOPIC}; they cannot send.
 */
@Configuration
@ConditionalOnWebApplication
@EnableWebSocketMessageBroker
class RealtimeConfig implements WebSocketMessageBrokerConfigurer {

	static final String TOPIC = "/topic/changes";

	private final AppProperties app;

	private final JwtDecoder jwtDecoder;

	RealtimeConfig(AppProperties app, JwtDecoder jwtDecoder) {
		this.app = app;
		this.jwtDecoder = jwtDecoder;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOrigins(app.webOrigin());
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
				if (accessor == null) {
					return message;
				}
				var command = accessor.getCommand();
				if (StompCommand.CONNECT.equals(command)) {
					accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
				}
				else if (StompCommand.SEND.equals(command)
						|| (StompCommand.SUBSCRIBE.equals(command) && !TOPIC.equals(accessor.getDestination()))) {
					throw new MessagingException("Not allowed");
				}
				return message;
			}
		});
	}

	private JwtAuthenticationToken authenticate(String header) {
		if (header == null || !header.startsWith("Bearer ")) {
			throw new MessagingException("Sign in required");
		}
		try {
			return new JwtAuthenticationToken(jwtDecoder.decode(header.substring(7)));
		}
		catch (JwtException e) {
			throw new MessagingException("Sign in required");
		}
	}

}
