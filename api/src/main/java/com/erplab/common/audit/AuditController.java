package com.erplab.common.audit;

import java.time.Instant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erplab.common.http.PageResult;
import com.erplab.common.infrastructure.Params;
import com.erplab.common.infrastructure.SqlPage;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "audit")
@RestController
@RequestMapping("/audit-log")
@PreAuthorize("hasAuthority('users:manage')")
class AuditController {

	record AuditEntry(Long id, Instant at, String username, String context, String action, Long targetId,
			String details) {
	}

	private final SqlPage sql;

	AuditController(SqlPage sql) {
		this.sql = sql;
	}

	@GetMapping
	PageResult<AuditEntry> list(@RequestParam(required = false) String username,
			@RequestParam(required = false) String context, @RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		return sql.query("id, at, username, context, action, target_id, details::text AS details", """
				audit_log WHERE (CAST(:username AS text) IS NULL OR username = :username)
				  AND (CAST(:context AS text) IS NULL OR context = :context)
				  AND (action ILIKE :q OR details::text ILIKE :q)""",
				Params.of("username", username, "context", context, "q", SqlPage.like(q)), "id DESC", page, size,
				AuditEntry.class);
	}

}
