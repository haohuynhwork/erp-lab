package com.erplab.auth.application.command;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erplab.auth.application.command.AuthCommands.*;
import com.erplab.auth.domain.User;
import com.erplab.auth.domain.UserRepository;
import com.erplab.auth.infrastructure.TokenService;
import com.erplab.auth.infrastructure.TokenService.BadCredentials;
import com.erplab.auth.infrastructure.TokenService.Tokens;
import com.erplab.common.domain.DomainException;
import com.erplab.common.domain.NotFoundException;

@Service
@Transactional
public class AuthCommandHandler {

	private final UserRepository users;

	private final PasswordEncoder passwords;

	private final TokenService tokens;

	AuthCommandHandler(UserRepository users, PasswordEncoder passwords, TokenService tokens) {
		this.users = users;
		this.passwords = passwords;
		this.tokens = tokens;
	}

	@Transactional(readOnly = true)
	public Tokens handle(Login cmd) {
		var user = users.findByUsernameAndDeletedAtIsNull(cmd.username().trim().toLowerCase())
			.filter(User::canSignIn)
			.filter(u -> passwords.matches(cmd.password(), u.getPasswordHash()))
			.orElseThrow(BadCredentials::new);
		return tokens.issue(user);
	}

	@Transactional(readOnly = true)
	public Tokens handle(Refresh cmd) {
		var username = tokens.verifyRefresh(cmd.refreshToken());
		var user = users.findByUsernameAndDeletedAtIsNull(username).filter(User::canSignIn)
			.orElseThrow(BadCredentials::new);
		return tokens.issue(user);
	}

	public Long handle(CreateUser cmd) {
		if (users.existsByUsernameAndDeletedAtIsNull(cmd.username().trim().toLowerCase())) {
			throw new DomainException("username_taken", "Username " + cmd.username() + " is taken");
		}
		var user = new User(cmd.username(), passwords.encode(cmd.password()), cmd.fullName(), cmd.email(),
				cmd.roles());
		return users.save(user).getId();
	}

	public void handle(Long id, UpdateUser cmd) {
		load(id).update(cmd.fullName(), cmd.email(), cmd.roles(), cmd.enabled());
	}

	public void handle(Long id, ChangePassword cmd) {
		load(id).changePassword(passwords.encode(cmd.password()));
	}

	public void delete(Long id) {
		load(id).delete();
	}

	private User load(Long id) {
		return users.findById(id).filter(u -> !u.isDeleted()).orElseThrow(() -> new NotFoundException("User", id));
	}

}
