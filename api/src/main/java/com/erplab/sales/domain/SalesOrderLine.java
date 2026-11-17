package com.erplab.sales.domain;

import java.math.BigDecimal;

import com.erplab.common.domain.Quantities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "sales_order_line")
public class SalesOrderLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long productId;

	private BigDecimal quantity;

	private BigDecimal unitPrice;

	private BigDecimal deliveredQuantity = BigDecimal.ZERO;

	private BigDecimal invoicedQuantity = BigDecimal.ZERO;

	protected SalesOrderLine() {
	}

	SalesOrderLine(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
		this.productId = productId;
		this.quantity = Quantities.requirePositive(quantity, "quantity");
		this.unitPrice = unitPrice;
	}

	public BigDecimal amount() {
		return Quantities.money(quantity.multiply(unitPrice));
	}

	public BigDecimal toDeliver() {
		return quantity.subtract(deliveredQuantity);
	}

	public BigDecimal toInvoice() {
		return deliveredQuantity.subtract(invoicedQuantity);
	}

	void deliver(BigDecimal qty) {
		deliveredQuantity = deliveredQuantity.add(qty);
	}

	void invoiced(BigDecimal qty) {
		invoicedQuantity = invoicedQuantity.add(qty);
	}

}
