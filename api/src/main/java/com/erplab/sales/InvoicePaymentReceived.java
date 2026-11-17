package com.erplab.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Customer paid (part of) an invoice. */
public record InvoicePaymentReceived(Long invoiceId, String invoiceNumber, Long customerId, LocalDate date,
		BigDecimal amount, String method) {
}
