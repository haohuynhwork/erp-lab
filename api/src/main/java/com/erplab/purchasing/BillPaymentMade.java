package com.erplab.purchasing;

import java.math.BigDecimal;
import java.time.LocalDate;

/** We paid (part of) a supplier bill (accounting: Dr payables / Cr cash). */
public record BillPaymentMade(Long billId, String billNumber, Long supplierId, LocalDate date, BigDecimal amount,
		String method) {
}
