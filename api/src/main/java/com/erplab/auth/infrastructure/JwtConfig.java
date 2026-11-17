package com.erplab.auth.infrastructure;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** HS256 tokens signed with {@code JWT_SECRET}; the {@code perms} claim becomes the authorities. */
@Configuration
class JwtConfig {

	private final SecretKey key;

	JwtConfig(@Value("${app.jwt.secret}") String secret) {
		if (secret.length() < 32) {
			throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
		}
		this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder() {
		return new NimbusJwtEncoder(new ImmutableSecret<>(key));
	}

	@Bean
	JwtDecoder jwtDecoder() {
		var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
		// refresh tokens must not be usable as access tokens
		decoder.setJwtValidator(jwt -> "access".equals(jwt.getClaimAsString("typ"))
				? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
				: org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
						new org.springframework.security.oauth2.core.OAuth2Error("invalid_token")));
		return decoder;
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		var converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			List<String> perms = jwt.getClaimAsStringList("perms");
			Collection<GrantedAuthority> authorities = perms == null ? List.of()
					: perms.stream().<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
			return authorities;
		});
		return converter;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
