package com.erplab.auth.http;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.erplab.auth.application.command.AuthCommandHandler;
import com.erplab.auth.application.command.AuthCommands.ChangePassword;
import com.erplab.auth.application.command.AuthCommands.CreateUser;
import com.erplab.auth.application.command.AuthCommands.UpdateUser;
import com.erplab.auth.application.query.UserQueries;
import com.erplab.auth.application.query.UserQueries.UserView;
import com.erplab.common.http.PageResult;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "auth")
@RestController
@RequestMapping("/users")
@PreAuthorize("hasAuthority('users:manage')")
class UserController {

	private final AuthCommandHandler commands;

	private final UserQueries queries;

	UserController(AuthCommandHandler commands, UserQueries queries) {
		this.commands = commands;
		this.queries = queries;
	}

	@GetMapping
	PageResult<UserView> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		return queries.list(page, size);
	}

	@GetMapping("/{id}")
	UserView get(@PathVariable Long id) {
		return queries.get(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	Map<String, Long> create(@Valid @RequestBody CreateUser cmd) {
		return Map.of("id", commands.handle(cmd));
	}

	@PutMapping("/{id}")
	UserView update(@PathVariable Long id, @Valid @RequestBody UpdateUser cmd) {
		commands.handle(id, cmd);
		return queries.get(id);
	}

	@PutMapping("/{id}/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void changePassword(@PathVariable Long id, @Valid @RequestBody ChangePassword cmd) {
		commands.handle(id, cmd);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable Long id) {
		commands.delete(id);
	}

}
