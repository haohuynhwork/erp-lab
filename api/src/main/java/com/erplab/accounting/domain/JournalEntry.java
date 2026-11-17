package com.erplab.accounting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.erplab.common.domain.BaseEntity;
import com.erplab.common.domain.DomainException;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;

/** A posted, balanced journal entry. Never edited: mistakes are fixed with a reversal. */
@Getter
@Entity
@Table(name = "journal_entry")
public class JournalEntry extends BaseEntity {

	private String number;

	private LocalDate entryDate;

	private String description;

	/** What caused it: MANUAL, PURCHASE_ORDER, INVOICE, PAYMENT, SALES_ORDER, ... */
	private String sourceType;

	private Long sourceId;

	private String sourceNumber;

	private Long reversalOfId;

	private boolean reversed;

	@ElementCollection
	@CollectionTable(name = "journal_line", joinColumns = @JoinColumn(name = "journal_entry_id"))
	@OrderColumn(name = "position")
	private List<JournalLine> lines = new ArrayList<>();

	protected JournalEntry() {
	}

	public JournalEntry(String number, LocalDate entryDate, String description, String sourceType, Long sourceId,
			String sourceNumber, List<JournalLine> lines) {
		var debit = BigDecimal.ZERO;
		var credit = BigDecimal.ZERO;
		for (var line : lines) {
			boolean oneSided = (line.debit().signum() > 0) != (line.credit().signum() > 0);
			if (!oneSided || line.debit().signum() < 0 || line.credit().signum() < 0) {
				throw new DomainException("invalid_line", "Each line needs either a positive debit or a positive credit");
			}
			debit = debit.add(line.debit());
			credit = credit.add(line.credit());
		}
		if (lines.size() < 2) {
			throw new DomainException("too_few_lines", "A journal entry needs at least two lines");
		}
		if (debit.compareTo(credit) != 0) {
			throw new DomainException("unbalanced", "Debits (%s) must equal credits (%s)".formatted(debit, credit));
		}
		this.number = number;
		this.entryDate = entryDate;
		this.description = description;
		this.sourceType = sourceType;
		this.sourceId = sourceId;
		this.sourceNumber = sourceNumber;
		this.lines.addAll(lines);
	}

	public JournalEntry reverse(String number, LocalDate date) {
		if (reversed || reversalOfId != null) {
			throw new DomainException("already_reversed", "This entry is already reversed or is itself a reversal");
		}
		reversed = true;
		var reversal = new JournalEntry(number, date, "Reversal of " + this.number, sourceType, sourceId, sourceNumber,
				lines.stream().map(JournalLine::reversed).toList());
		reversal.reversalOfId = getId();
		return reversal;
	}

}
