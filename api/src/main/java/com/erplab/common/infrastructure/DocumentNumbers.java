package com.erplab.common.infrastructure;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Next document number from a Postgres sequence, e.g. {@code PO-000042}. */
@Component
public class DocumentNumbers {

	private final JdbcClient jdbc;

	DocumentNumbers(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** @param sequence one of the {@code *_number_seq} sequences created by Flyway */
	public String next(String prefix, String sequence) {
		if (!sequence.matches("[a-z_]+")) {
			throw new IllegalArgumentException("Bad sequence name " + sequence);
		}
		long n = jdbc.sql("SELECT nextval('" + sequence + "')").query(Long.class).single();
		return "%s-%06d".formatted(prefix, n);
	}

}
