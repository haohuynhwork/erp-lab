package com.erplab.auth.infrastructure;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.erplab.auth.domain.User;

@Component
public class TokenService {

	public record Tokens(String accessToken, String refreshToken, long expiresIn) {
	}

	private final JwtEncoder encoder;

	private final NimbusJwtDecoder refreshDecoder;

	private final Duration accessTtl;

	private final Duration refreshTtl;

	TokenService(JwtEncoder encoder, @Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.access-ttl:15m}") Duration accessTtl,
			@Value("${app.jwt.refresh-ttl:7d}") Duration refreshTtl) {
		this.encoder = encoder;
		this.refreshDecoder = NimbusJwtDecoder
			.withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		this.accessTtl = accessTtl;
		this.refreshTtl = refreshTtl;
	}

	public Tokens issue(User user) {
		var now = Instant.now();
		var access = JwtClaimsSet.builder()
			.subject(user.getUsername())
			.issuedAt(now)
			.expiresAt(now.plus(accessTtl))
			.claim("typ", "access")
			.claim("uid", user.getId())
			.claim("perms", user.permissions())
			.build();
		var refresh = JwtClaimsSet.builder()
			.subject(user.getUsername())
			.issuedAt(now)
			.expiresAt(now.plus(refreshTtl))
			.claim("typ", "refresh")
			.build();
		return new Tokens(encode(access), encode(refresh), accessTtl.toSeconds());
	}

	/** @return the username in a valid refresh token */
	public String verifyRefresh(String token) {
		Jwt jwt;
		try {
			jwt = refreshDecoder.decode(token);
		}
		catch (JwtException e) {
			throw new BadCredentials();
		}
		if (!"refresh".equals(jwt.getClaimAsString("typ"))) {
			throw new BadCredentials();
		}
		return jwt.getSubject();
	}

	private String encode(JwtClaimsSet claims) {
		var header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public static class BadCredentials extends com.erplab.common.domain.UnauthorizedException {

		public BadCredentials() {
			super("bad_credentials", "Wrong username or password");
		}

	}

}
