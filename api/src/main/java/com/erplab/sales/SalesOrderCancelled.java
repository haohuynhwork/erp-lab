package com.erplab.sales;

import java.math.BigDecimal;
import java.util.List;

/** A confirmed sales order was cancelled: inventory releases its reservations. */
public record SalesOrderCancelled(Long salesOrderId, String salesOrderNumber, Long warehouseId, List<Line> lines) {

	public record Line(Long productId, BigDecimal quantity) {
	}

}
