package com.erplab.sales;

import java.math.BigDecimal;
import java.util.List;

/** A sales order was confirmed: inventory reserves its quantities. */
public record SalesOrderConfirmed(Long salesOrderId, String salesOrderNumber, Long warehouseId, List<Line> lines) {

	public record Line(Long productId, BigDecimal quantity) {
	}

}
