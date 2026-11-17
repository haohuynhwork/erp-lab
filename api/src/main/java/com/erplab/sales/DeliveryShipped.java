package com.erplab.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Goods of a sales order left a warehouse (published by sales, used by inventory). */
public record DeliveryShipped(Long salesOrderId, String salesOrderNumber, Long customerId, Long warehouseId,
		LocalDate date, List<Line> lines) {

	public record Line(Long productId, BigDecimal quantity) {
	}

}
