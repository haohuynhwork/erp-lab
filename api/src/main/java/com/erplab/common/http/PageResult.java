package com.erplab.common.http;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** One page of a list endpoint: {@code data} = {items, total, page, size}. */
public record PageResult<T>(List<T> items, long total, int page, int size) {

	public static <T> PageResult<T> of(Page<T> page) {
		return new PageResult<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize());
	}

	public static <E, T> PageResult<T> of(Page<E> page, Function<E, T> mapper) {
		return of(page.map(mapper));
	}

	public static <T> PageResult<T> of(List<T> all) {
		return new PageResult<>(all, all.size(), 0, all.size());
	}

}
