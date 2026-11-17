package com.erplab.accounting.http;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.erplab.accounting.application.command.AccountingCommandHandler;
import com.erplab.accounting.application.command.AccountingCommands.*;
import com.erplab.accounting.application.query.AccountingQueries;
import com.erplab.accounting.application.query.AccountingQueries.*;
import com.erplab.common.http.PageResult;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "accounting")
@RestController
@RequestMapping("/accounting")
@PreAuthorize("hasAuthority('accounting:read')")
class AccountingController {

	private static final String WRITE = "hasAuthority('accounting:write')";

	private final AccountingCommandHandler commands;

	private final AccountingQueries queries;

	AccountingController(AccountingCommandHandler commands, AccountingQueries queries) {
		this.commands = commands;
		this.queries = queries;
	}

	@GetMapping("/accounts")
	PageResult<AccountView> accounts(@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "200") int size) {
		return queries.accounts(q, page, size);
	}

	@PostMapping("/accounts")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(WRITE)
	Map<String, Long> createAccount(@Valid @RequestBody CreateAccount cmd) {
		return Map.of("id", commands.createAccount(cmd));
	}

	@PutMapping("/accounts/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(WRITE)
	void updateAccount(@PathVariable Long id, @Valid @RequestBody UpdateAccount cmd) {
		commands.updateAccount(id, cmd);
	}

	@GetMapping("/trial-balance")
	TrialBalance trialBalance(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return queries.trialBalance(from, to);
	}

	@GetMapping("/journal-entries")
	PageResult<JournalEntryView> entries(@RequestParam(required = false) String q,
			@RequestParam(required = false) String sourceType, @RequestParam(required = false) Long accountId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		return queries.entries(q, sourceType, accountId, from, to, page, size);
	}

	@GetMapping("/journal-entries/{id}")
	JournalEntryDetail entry(@PathVariable Long id) {
		return queries.entry(id);
	}

	@PostMapping("/journal-entries")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(WRITE)
	Map<String, Long> post(@Valid @RequestBody PostManualEntry cmd) {
		return Map.of("id", commands.postManual(cmd));
	}

	@PostMapping("/journal-entries/{id}/reverse")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(WRITE)
	Map<String, Long> reverse(@PathVariable Long id) {
		return Map.of("id", commands.reverse(id));
	}

	/** @param kind receivable (customers owe us) or payable (we owe suppliers) */
	@GetMapping("/balances/{kind}")
	List<PartnerBalance> balances(@PathVariable String kind) {
		return queries.partnerBalances(kind);
	}

	@GetMapping("/profit-and-loss")
	ProfitAndLoss profitAndLoss(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return queries.profitAndLoss(from, to);
	}

	@GetMapping("/balance-sheet")
	BalanceSheet balanceSheet(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
		return queries.balanceSheet(asOf != null ? asOf : LocalDate.now());
	}

}
