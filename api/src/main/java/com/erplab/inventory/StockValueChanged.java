package com.erplab.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Inventory value changed by {@code amount} (positive = more stock value). Accounting posts it, except
 * {@link Reason#PURCHASE_RECEIPT}, which it books from purchasing's GoodsReceived at the order price.
 */
public record StockValueChanged(Reason reason, BigDecimal amount, String refType, Long refId, String refNumber,
		LocalDate date) {

	public enum Reason {
		PURCHASE_RECEIPT, SALES_ISSUE, MANUAL_RECEIPT, MANUAL_ISSUE, ADJUSTMENT
	}

}
