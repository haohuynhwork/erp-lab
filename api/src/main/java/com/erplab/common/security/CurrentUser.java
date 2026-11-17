package com.erplab.common.security;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Username of the signed-in user (JWT subject); "system" for seeders and event handlers without one. */
@Component
public class CurrentUser implements AuditorAware<String> {

	public static String username() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth == null || !auth.isAuthenticated() ? "system" : auth.getName();
	}

	@Override
	public Optional<String> getCurrentAuditor() {
		return Optional.of(username());
	}

}
