package com.erplab.accounting.domain;

public enum AccountType {

	ASSET(true), LIABILITY(false), EQUITY(false), REVENUE(false), EXPENSE(true);

	private final boolean debitNormal;

	AccountType(boolean debitNormal) {
		this.debitNormal = debitNormal;
	}

	/** Assets and expenses grow with debits; the others with credits. */
	public boolean debitNormal() {
		return debitNormal;
	}

}
