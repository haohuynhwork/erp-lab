package com.erplab.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Part of an invoice was credited (accounting: Dr revenue, Dr output VAT / Cr receivables). */
public record CreditNoteIssued(Long creditNoteId, String creditNoteNumber, String invoiceNumber, Long customerId,
		LocalDate date, BigDecimal subtotal, BigDecimal taxAmount, BigDecimal total) {
}
