package com.erplab.common.domain;

/** Returned to clients as {@code fail} (HTTP 401). */
public class UnauthorizedException extends DomainException {

	public UnauthorizedException(String code, String message) {
		super(code, message);
	}

}
