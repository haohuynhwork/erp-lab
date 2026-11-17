package com.erplab.purchasing.domain;

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
@Table(name = "purchase_order_line")
public class PurchaseOrderLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long productId;

	private BigDecimal quantity;

	private BigDecimal unitPrice;

	private BigDecimal receivedQuantity = BigDecimal.ZERO;

	protected PurchaseOrderLine() {
	}

	PurchaseOrderLine(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
		this.productId = productId;
		this.quantity = Quantities.requirePositive(quantity, "quantity");
		this.unitPrice = unitPrice;
	}

	public BigDecimal amount() {
		return Quantities.money(quantity.multiply(unitPrice));
	}

	public BigDecimal remaining() {
		return quantity.subtract(receivedQuantity);
	}

	void receive(BigDecimal qty) {
		receivedQuantity = receivedQuantity.add(qty);
	}

}
