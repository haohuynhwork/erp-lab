package com.erplab.common.config;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
class SecurityConfig {

	// Bearer JWT (auth context issues and decodes it); permissions checked with @PreAuthorize.
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(csrf -> csrf.disable())
			.cors(Customizer.withDefaults())
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/v1/system/**", "/v1/auth/login", "/v1/auth/refresh", "/docs/**", "/swagger-ui/**", "/graphql", "/graphiql", "/actuator/health/**")
				.permitAll()
				.anyRequest()
				.authenticated())
			.oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()).authenticationEntryPoint(SecurityConfig::unauthorized))
			.exceptionHandling(e -> e.authenticationEntryPoint(SecurityConfig::unauthorized))
			.build();
	}

	/** 401 in the same {@code fail} shape as {@code ResponseEnvelope}. */
	private static void unauthorized(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException e) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter()
			.write("{\"status\":\"fail\",\"code\":\"unauthorized\",\"message\":\"Sign in required\"}");
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(AppProperties app) {
		var cors = new CorsConfiguration();
		cors.setAllowedOrigins(List.of(app.webOrigin()));
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("*"));
		cors.setAllowCredentials(true);
		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cors);
		return source;
	}

}
