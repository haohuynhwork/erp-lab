package com.erplab.inventory.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** {@code date} (accounting date of the value change) may be omitted: today. */
public final class InventoryCommands {

	private InventoryCommands() {
	}

	/** Receipt without a purchase order, e.g. opening stock. */
	public record ReceiveStock(@NotNull Long productId, @NotNull Long warehouseId, @NotNull @Positive BigDecimal quantity,
			@NotNull @PositiveOrZero BigDecimal unitCost, String note, LocalDate date) {
	}

	/** Issue without a sales order, e.g. scrap or internal use. */
	public record IssueStock(@NotNull Long productId, @NotNull Long warehouseId, @NotNull @Positive BigDecimal quantity,
			String note, LocalDate date) {
	}

	public record TransferStock(@NotNull Long productId, @NotNull Long fromWarehouseId, @NotNull Long toWarehouseId,
			@NotNull @Positive BigDecimal quantity, String note) {
	}

	/** Physical count: sets the on-hand quantity. */
	public record CountStock(@NotNull Long productId, @NotNull Long warehouseId,
			@NotNull @PositiveOrZero BigDecimal countedQuantity, String note, LocalDate date) {
	}

}
