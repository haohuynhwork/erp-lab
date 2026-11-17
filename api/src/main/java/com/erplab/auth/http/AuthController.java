package com.erplab.auth.http;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erplab.auth.application.command.AuthCommandHandler;
import com.erplab.auth.application.command.AuthCommands.Login;
import com.erplab.auth.application.command.AuthCommands.Refresh;
import com.erplab.auth.application.query.UserQueries;
import com.erplab.auth.application.query.UserQueries.UserView;
import com.erplab.auth.infrastructure.TokenService.Tokens;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "auth")
@RestController
@RequestMapping("/auth")
class AuthController {

	private final AuthCommandHandler commands;

	private final UserQueries queries;

	AuthController(AuthCommandHandler commands, UserQueries queries) {
		this.commands = commands;
		this.queries = queries;
	}

	@PostMapping("/login")
	Tokens login(@Valid @RequestBody Login cmd) {
		return commands.handle(cmd);
	}

	@PostMapping("/refresh")
	Tokens refresh(@Valid @RequestBody Refresh cmd) {
		return commands.handle(cmd);
	}

	@GetMapping("/me")
	UserView me(Authentication auth) {
		return queries.byUsername(auth.getName());
	}

}
