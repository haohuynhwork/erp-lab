package com.erplab.common.infrastructure;

import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.erplab.common.http.PageResult;

/** Paged read-model queries: {@code SELECT <columns> FROM <from> ORDER BY <order> LIMIT/OFFSET}. */
@Component
public class SqlPage {

	private final JdbcClient jdbc;

	SqlPage(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * @param from FROM clause incl. joins and WHERE, with named parameters
	 * @param orderBy trusted ORDER BY expression (never user input)
	 */
	public <T> PageResult<T> query(String columns, String from, Map<String, ?> params, String orderBy, int page,
			int size, Class<T> type) {
		int limit = Math.clamp(size, 1, 500);
		int offset = Math.max(page, 0) * limit;
		long total = jdbc.sql("SELECT count(*) FROM " + from).params(params).query(Long.class).single();
		var items = jdbc.sql("SELECT " + columns + " FROM " + from + " ORDER BY " + orderBy + " LIMIT " + limit
				+ " OFFSET " + offset)
			.params(params)
			.query(type)
			.list();
		return new PageResult<>(items, total, Math.max(page, 0), limit);
	}

	/** {@code %term%} for ILIKE, or {@code %} for no filter. */
	public static String like(String term) {
		return term == null || term.isBlank() ? "%" : "%" + term.trim().replace("%", "\\%") + "%";
	}

}
