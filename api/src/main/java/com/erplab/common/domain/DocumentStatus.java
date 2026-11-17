package com.erplab.common.domain;

/** Lifecycle shared by business documents: Draft → Confirmed → Done, or Cancelled before Done. */
public enum DocumentStatus {

	DRAFT, CONFIRMED, DONE, CANCELLED;

	public void requireIs(DocumentStatus expected, String action) {
		if (this != expected) {
			throw new DomainException("invalid_status", "Cannot " + action + " a document that is " + this);
		}
	}

	public boolean isOpen() {
		return this == DRAFT || this == CONFIRMED;
	}

}
