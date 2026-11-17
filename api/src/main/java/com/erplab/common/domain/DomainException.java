package com.erplab.common.domain;

/** A broken business rule; returned to clients as {@code fail} with its code (HTTP 422). */
public class DomainException extends RuntimeException {

	private final String code;

	public DomainException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}

}
