package com.erplab.sales.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public final class SalesCommands {

	private SalesCommands() {
	}

	/** @param unitPrice null = the product's sale price */
	public record LineInput(@NotNull Long productId, @NotNull @Positive BigDecimal quantity,
			@PositiveOrZero BigDecimal unitPrice) {
	}

	/** Create or edit (Draft only). */
	public record SaveSalesOrder(@NotNull Long customerId, @NotNull Long warehouseId, @NotNull LocalDate orderDate,
			LocalDate deliveryDate, @NotNull @PositiveOrZero @DecimalMax("100") BigDecimal taxRate, String note,
			@NotEmpty List<@Valid LineInput> lines) {
	}

	public record DeliveryLine(@NotNull Long lineId, @NotNull @PositiveOrZero BigDecimal quantity) {
	}

	/** @param date null = today */
	public record Deliver(LocalDate date, @NotEmpty List<@Valid DeliveryLine> lines) {
	}

	/** @param date null = today */
	public record CreateInvoice(LocalDate date) {
	}

	public record RecordPayment(@NotNull @Positive BigDecimal amount, LocalDate date, @NotBlank String method,
			String reference) {
	}

}
