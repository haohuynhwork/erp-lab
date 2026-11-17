package com.erplab.common.audit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.erplab.common.security.CurrentUser;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Writes an {@code audit_log} row for every successful command: public methods of the command handlers
 * ({@code com.erplab.<context>.application.command}), except event listeners (their command is already logged).
 * Arguments are stored as JSON with passwords and tokens removed.
 */
@Aspect
@Component
class CommandAudit {

	private static final Logger log = LoggerFactory.getLogger(CommandAudit.class);

	private static final Set<String> SECRET_FIELDS = Set.of("password", "refreshToken");

	/** Token refreshes happen every 15 minutes per user; not worth a row each. */
	private static final Set<String> SKIPPED = Set.of("Refresh");

	private final JdbcClient jdbc;

	private final JsonMapper json;

	private final TransactionTemplate newTransaction;

	CommandAudit(JdbcClient jdbc, JsonMapper json, PlatformTransactionManager transactions) {
		this.jdbc = jdbc;
		this.json = json;
		this.newTransaction = new TransactionTemplate(transactions);
		this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@AfterReturning(pointcut = "within(com.erplab..application.command..*) && execution(public * *(..))"
			+ " && !@annotation(org.springframework.context.event.EventListener)", returning = "result")
	void audit(JoinPoint jp, Object result) {
		try {
			var signature = (MethodSignature) jp.getSignature();
			var args = jp.getArgs();
			Long targetId = null;
			var details = new LinkedHashMap<String, Object>();
			String action = signature.getName();
			for (var arg : args) {
				if (arg instanceof Long id && targetId == null) {
					targetId = id;
				}
				else if (arg != null) {
					if (SKIPPED.contains(arg.getClass().getSimpleName())) {
						return;
					}
					// handle(CreateUser) → action "CreateUser"
					if (action.equals("handle")) {
						action = arg.getClass().getSimpleName();
					}
					details.put(arg.getClass().getSimpleName(), clean(arg));
				}
			}
			if (targetId == null && result instanceof Long id) {
				targetId = id;
			}
			var context = signature.getDeclaringType().getPackageName().split("\\.")[2];
			var user = CurrentUser.username();
			if ("system".equals(user) && details.get("Login") instanceof Map<?, ?> login) {
				user = String.valueOf(login.get("username"));
			}
			var insert = jdbc.sql("""
					INSERT INTO audit_log (username, context, action, target_id, details)
					VALUES (:user, :context, :action, :target, CAST(:details AS jsonb))""")
				.param("user", user)
				.param("context", context)
				.param("action", action)
				.param("target", targetId)
				.param("details", json.writeValueAsString(details));
			// normally part of the command's transaction; read-only ones (login) can't insert, so use a new one
			if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
				newTransaction.executeWithoutResult(status -> insert.update());
			}
			else {
				insert.update();
			}
		}
		catch (RuntimeException e) {
			// never fail a business operation because of auditing
			log.warn("Could not write audit log", e);
		}
	}

	private Object clean(Object arg) {
		var node = json.valueToTree(arg);
		if (node instanceof ObjectNode object) {
			SECRET_FIELDS.forEach(object::remove);
		}
		return json.convertValue(node, Object.class);
	}

}
