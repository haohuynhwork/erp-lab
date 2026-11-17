package com.erplab.purchasing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Goods of a purchase order arrived in a warehouse (published by purchasing, used by inventory and accounting). */
public record GoodsReceived(Long purchaseOrderId, String purchaseOrderNumber, Long supplierId, Long warehouseId,
		LocalDate date, List<Line> lines, BigDecimal amount) {

	public record Line(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
	}

}
