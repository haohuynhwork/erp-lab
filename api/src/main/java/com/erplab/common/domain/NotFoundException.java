package com.erplab.common.domain;

/** Returned to clients as {@code fail} with code {@code not_found} (HTTP 404). */
public class NotFoundException extends DomainException {

	public NotFoundException(String what, Object id) {
		super("not_found", what + " " + id + " not found");
	}

}
