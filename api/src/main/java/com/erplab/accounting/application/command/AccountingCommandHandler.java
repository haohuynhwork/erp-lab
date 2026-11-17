package com.erplab.accounting.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erplab.accounting.application.command.AccountingCommands.*;
import com.erplab.accounting.domain.*;
import com.erplab.common.domain.DomainException;
import com.erplab.common.domain.NotFoundException;
import com.erplab.common.infrastructure.DocumentNumbers;
import com.erplab.masterdata.MasterdataApi;

@Service
@Transactional
public class AccountingCommandHandler {

	private final AccountRepository accounts;

	private final JournalEntryRepository entries;

	private final SupplierPaymentRepository payments;

	private final MasterdataApi masterdata;

	private final DocumentNumbers numbers;

	private final JdbcClient jdbc;

	AccountingCommandHandler(AccountRepository accounts, JournalEntryRepository entries,
			SupplierPaymentRepository payments, MasterdataApi masterdata, DocumentNumbers numbers, JdbcClient jdbc) {
		this.accounts = accounts;
		this.entries = entries;
		this.payments = payments;
		this.masterdata = masterdata;
		this.numbers = numbers;
		this.jdbc = jdbc;
	}

	public Long createAccount(CreateAccount cmd) {
		var account = new Account(cmd.code(), cmd.type());
		account.update(cmd.name(), true);
		return accounts.save(account).getId();
	}

	public void updateAccount(Long id, UpdateAccount cmd) {
		accounts.findById(id).orElseThrow(() -> new NotFoundException("Account", id)).update(cmd.name(), cmd.active());
	}

	public Long postManual(PostManualEntry cmd) {
		var lines = cmd.lines().stream().map(l -> {
			var account = accounts.findById(l.accountId()).orElseThrow(() -> new NotFoundException("Account", l.accountId()));
			if (!account.isActive()) {
				throw new DomainException("inactive_account", "Account " + account.getCode() + " is inactive");
			}
			return new JournalLine(l.accountId(), l.partnerId(), l.debit(), l.credit(), l.memo());
		}).toList();
		return post(cmd.date(), cmd.description(), "MANUAL", null, null, lines);
	}

	public Long reverse(Long id) {
		var entry = entries.findById(id).orElseThrow(() -> new NotFoundException("Journal entry", id));
		return entries.save(entry.reverse(numbers.next("JE", "journal_entry_number_seq"), LocalDate.now())).getId();
	}

	/** Pays (part of) what we owe a supplier: Dr payables / Cr cash. */
	public Long paySupplier(PaySupplier cmd) {
		var supplier = masterdata.partner(cmd.supplierId());
		if (!supplier.supplier()) {
			throw new DomainException("not_a_supplier", "The partner is not a supplier");
		}
		var owed = payableBalance(cmd.supplierId());
		if (cmd.amount().compareTo(owed) > 0) {
			throw new DomainException("overpayment", "We owe %s only %s".formatted(supplier.name(), owed));
		}
		var date = cmd.date() != null ? cmd.date() : LocalDate.now();
		var payment = payments.save(new SupplierPayment(numbers.next("PAY", "supplier_payment_number_seq"),
				cmd.supplierId(), date, cmd.amount(), cmd.method(), cmd.reference()));
		post(date, "Payment to " + supplier.name(), "SUPPLIER_PAYMENT", payment.getId(), payment.getNumber(),
				List.of(JournalLine.debit(account(Account.PAYABLE), cmd.supplierId(), cmd.amount()),
						JournalLine.credit(account(Account.CASH), null, cmd.amount())));
		return payment.getId();
	}

	/** Used by {@link AutomaticPostings} and the commands above. */
	Long post(LocalDate date, String description, String sourceType, Long sourceId, String sourceNumber,
			List<JournalLine> lines) {
		var entry = new JournalEntry(numbers.next("JE", "journal_entry_number_seq"), date, description, sourceType,
				sourceId, sourceNumber, lines);
		return entries.save(entry).getId();
	}

	Long account(String code) {
		return accounts.findByCode(code).orElseThrow(() -> new NotFoundException("Account", code)).getId();
	}

	private BigDecimal payableBalance(Long supplierId) {
		return jdbc.sql("""
				SELECT coalesce(sum(l.credit - l.debit), 0) FROM journal_line l JOIN account a ON a.id = l.account_id
				WHERE a.code = :code AND l.partner_id = :partner""")
			.param("code", Account.PAYABLE)
			.param("partner", supplierId)
			.query(BigDecimal.class)
			.single();
	}

}
