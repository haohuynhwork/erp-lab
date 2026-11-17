package com.erplab.accounting.domain;

import java.math.BigDecimal;

import jakarta.persistence.Embeddable;

/** Exactly one of debit / credit is positive. {@code partnerId} tracks receivables and payables per partner. */
@Embeddable
public record JournalLine(Long accountId, Long partnerId, BigDecimal debit, BigDecimal credit, String memo) {

	public static JournalLine debit(Long accountId, Long partnerId, BigDecimal amount) {
		return new JournalLine(accountId, partnerId, amount, BigDecimal.ZERO, null);
	}

	public static JournalLine credit(Long accountId, Long partnerId, BigDecimal amount) {
		return new JournalLine(accountId, partnerId, BigDecimal.ZERO, amount, null);
	}

	JournalLine reversed() {
		return new JournalLine(accountId, partnerId, credit, debit, memo);
	}

}
