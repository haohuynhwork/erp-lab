package com.erplab.purchasing.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public final class PurchasingCommands {

	private PurchasingCommands() {
	}

	/** @param unitPrice null = the product's purchase price */
	public record PurchaseLineInput(@NotNull Long productId, @NotNull @Positive BigDecimal quantity,
			@PositiveOrZero BigDecimal unitPrice) {
	}

	/** Create or edit (Draft only). */
	public record SavePurchaseOrder(@NotNull Long supplierId, @NotNull Long warehouseId, @NotNull LocalDate orderDate,
			LocalDate expectedDate, String note, @NotEmpty List<@Valid PurchaseLineInput> lines) {
	}

	public record ReceiptLine(@NotNull Long lineId, @NotNull @PositiveOrZero BigDecimal quantity) {
	}

	/** @param date null = today */
	public record ReceiveGoods(LocalDate date, @NotEmpty List<@Valid ReceiptLine> lines) {
	}

	/** @param date null = today */
	public record CreateBill(String supplierReference, LocalDate date) {
	}

	public record PayBill(@NotNull @Positive BigDecimal amount, LocalDate date,
			@jakarta.validation.constraints.NotBlank String method, String reference) {
	}

}
