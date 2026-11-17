package com.erplab.accounting.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.erplab.accounting.domain.AccountType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class AccountingCommands {

	private AccountingCommands() {
	}

	public record CreateAccount(@NotBlank @Pattern(regexp = "\\d{3,10}") String code, @NotBlank String name,
			@NotNull AccountType type) {
	}

	public record UpdateAccount(@NotBlank String name, boolean active) {
	}

	public record ManualLine(@NotNull Long accountId, Long partnerId, @NotNull @PositiveOrZero BigDecimal debit,
			@NotNull @PositiveOrZero BigDecimal credit, String memo) {
	}

	public record PostManualEntry(@NotNull LocalDate date, @NotBlank String description,
			@NotNull @Size(min = 2) List<@Valid ManualLine> lines) {
	}

}
