package com.erplab.auth.infrastructure;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.erplab.auth.application.command.AuthCommandHandler;
import com.erplab.auth.application.command.AuthCommands.CreateUser;
import com.erplab.auth.domain.Role;
import com.erplab.auth.domain.UserRepository;

/** Creates the {@code admin} user (password {@code SEED_PASSWORD}) on an empty database. */
@Component
@Order(0)
class AdminSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

	private final UserRepository users;

	private final AuthCommandHandler commands;

	private final String password;

	AdminSeeder(UserRepository users, AuthCommandHandler commands, @Value("${app.seed-password}") String password) {
		this.users = users;
		this.commands = commands;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (users.count() == 0) {
			commands.handle(new CreateUser("admin", password, "Administrator", null, Set.of(Role.ADMIN)));
			log.info("Created user 'admin' with SEED_PASSWORD");
		}
	}

}
