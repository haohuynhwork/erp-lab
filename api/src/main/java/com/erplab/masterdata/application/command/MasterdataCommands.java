package com.erplab.masterdata.application.command;

import java.math.BigDecimal;
import java.util.List;

import com.erplab.masterdata.domain.ProductType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Create and update payloads of master data (create and update share one shape per entity). */
public final class MasterdataCommands {

	private MasterdataCommands() {
	}

	/** {@code active} may be omitted: null means active. */
	static boolean active(Boolean active) {
		return !Boolean.FALSE.equals(active);
	}

	public record SaveUnit(@NotBlank @Size(max = 20) String code, @NotBlank String name) {
	}

	public record SaveCategory(@NotBlank String name, String description) {
	}

	public record BomLineInput(@NotNull Long componentId, @NotNull @Positive BigDecimal quantity) {
	}

	public record SaveProduct(@NotBlank @Size(max = 50) String sku, @NotBlank String name, @NotNull ProductType type,
			@NotNull Long unitId, Long categoryId, @NotNull @PositiveOrZero BigDecimal purchasePrice,
			@NotNull @PositiveOrZero BigDecimal salePrice, @NotNull @PositiveOrZero BigDecimal minStock,
			Boolean active, List<@Valid BomLineInput> bom) {
	}

	public record SaveWarehouse(@NotBlank @Size(max = 20) String code, @NotBlank String name, String address,
			Boolean active) {
	}

	public record SavePartner(@NotBlank @Size(max = 20) String code, @NotBlank String name, boolean customer,
			boolean supplier, String taxCode, @Email String email, String phone, String address,
			@PositiveOrZero int paymentTermDays, Boolean active) {
	}

}
