package com.erplab.auth.application.command;

import java.util.Set;

import com.erplab.auth.domain.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Commands of the auth context. */
public final class AuthCommands {

	private AuthCommands() {
	}

	public record Login(@NotBlank String username, @NotBlank String password) {
	}

	public record Refresh(@NotBlank String refreshToken) {
	}

	public record CreateUser(@NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}") String username,
			@NotBlank @Size(min = 8, max = 100) String password, @NotBlank String fullName, @Email String email,
			@NotEmpty Set<Role> roles) {
	}

	public record UpdateUser(@NotBlank String fullName, @Email String email, @NotEmpty Set<Role> roles,
			@jakarta.validation.constraints.NotNull Boolean enabled) {
	}

	public record ChangePassword(@NotBlank @Size(min = 8, max = 100) String password) {
	}

}
