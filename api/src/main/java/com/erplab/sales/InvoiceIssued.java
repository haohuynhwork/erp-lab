package com.erplab.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Customer invoice issued: receivable {@code total} = revenue {@code subtotal} + output VAT {@code taxAmount}. */
public record InvoiceIssued(Long invoiceId, String invoiceNumber, Long customerId, LocalDate date, BigDecimal subtotal,
		BigDecimal taxAmount, BigDecimal total) {
}
