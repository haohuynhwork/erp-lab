package com.erplab.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** Immutable ledger row: every change of a {@link StockItem}. Quantity and value are signed. */
@Getter
@Entity
@Table(name = "stock_movement")
public class StockMovement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long productId;

	private Long warehouseId;

	@Enumerated(EnumType.STRING)
	private MovementType type;

	private BigDecimal quantity;

	private BigDecimal value;

	private String refType;

	private Long refId;

	private String refNumber;

	private String note;

	private Instant movedAt;

	private String movedBy;

	protected StockMovement() {
	}

	public StockMovement(StockItem item, MovementType type, BigDecimal quantity, BigDecimal value, String refType,
			Long refId, String refNumber, String note, String movedBy) {
		this.productId = item.getProductId();
		this.warehouseId = item.getWarehouseId();
		this.type = type;
		this.quantity = quantity;
		this.value = value;
		this.refType = refType;
		this.refId = refId;
		this.refNumber = refNumber;
		this.note = note;
		this.movedAt = Instant.now();
		this.movedBy = movedBy;
	}

}
