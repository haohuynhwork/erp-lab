package com.erplab.accounting.application.command;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erplab.accounting.application.command.AccountingCommands.*;
import com.erplab.accounting.domain.*;
import com.erplab.common.domain.DomainException;
import com.erplab.common.domain.NotFoundException;
import com.erplab.common.infrastructure.DocumentNumbers;

@Service
@Transactional
public class AccountingCommandHandler {

	private final AccountRepository accounts;

	private final JournalEntryRepository entries;

	private final DocumentNumbers numbers;

	AccountingCommandHandler(AccountRepository accounts, JournalEntryRepository entries, DocumentNumbers numbers) {
		this.accounts = accounts;
		this.entries = entries;
		this.numbers = numbers;
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

}
