package com.erplab.inventory.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.erplab.common.domain.BaseEntity;
import com.erplab.common.domain.DomainException;
import com.erplab.common.domain.Quantities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/** On-hand quantity of one product in one warehouse, valued at moving average cost. Never negative. */
@Getter
@Entity
@Table(name = "stock_item")
public class StockItem extends BaseEntity {

	private Long productId;

	private Long warehouseId;

	private BigDecimal quantity = BigDecimal.ZERO;

	private BigDecimal averageCost = BigDecimal.ZERO;

	/** Promised to confirmed sales orders, not yet delivered. May exceed quantity (a shortage). */
	private BigDecimal reserved = BigDecimal.ZERO;

	protected StockItem() {
	}

	public StockItem(Long productId, Long warehouseId) {
		this.productId = productId;
		this.warehouseId = warehouseId;
	}

	/** @return value added */
	public BigDecimal receive(BigDecimal qty, BigDecimal unitCost) {
		Quantities.requirePositive(qty, "quantity");
		if (unitCost.signum() < 0) {
			throw new DomainException("negative_cost", "Unit cost cannot be negative");
		}
		var newQty = quantity.add(qty);
		averageCost = quantity.multiply(averageCost).add(qty.multiply(unitCost)).divide(newQty, 4, RoundingMode.HALF_UP);
		quantity = newQty;
		return Quantities.money(qty.multiply(unitCost));
	}

	/** @return value removed (at average cost) */
	public BigDecimal issue(BigDecimal qty) {
		Quantities.requirePositive(qty, "quantity");
		if (quantity.compareTo(qty) < 0) {
			throw new DomainException("insufficient_stock",
					"Only %s in stock, cannot issue %s".formatted(quantity.stripTrailingZeros().toPlainString(),
							qty.stripTrailingZeros().toPlainString()));
		}
		quantity = quantity.subtract(qty);
		return Quantities.money(qty.multiply(averageCost));
	}

	/** Sets the quantity to a physical count. @return signed value change */
	public BigDecimal countTo(BigDecimal counted) {
		if (counted == null || counted.signum() < 0) {
			throw new DomainException("invalid_quantity", "Counted quantity cannot be negative");
		}
		var delta = counted.subtract(quantity);
		quantity = counted;
		return Quantities.money(delta.multiply(averageCost));
	}

	public BigDecimal available() {
		return quantity.subtract(reserved);
	}

	public void reserve(BigDecimal qty) {
		reserved = reserved.add(Quantities.requirePositive(qty, "quantity"));
	}

	/** Releases up to {@code qty} (never below zero: deliveries may exceed what was reserved). */
	public void release(BigDecimal qty) {
		reserved = reserved.subtract(qty.min(reserved));
	}

}
