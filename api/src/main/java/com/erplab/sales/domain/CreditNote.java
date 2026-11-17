package com.erplab.sales.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.erplab.common.domain.BaseEntity;
import com.erplab.sales.CreditNoteIssued;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;

/** Credits part of an invoice (price correction, returned or rejected goods). Created by {@link Invoice#credit}. */
@Getter
@Entity
@Table(name = "credit_note")
public class CreditNote extends BaseEntity {

	private String number;

	private Long invoiceId;

	private String invoiceNumber;

	private Long customerId;

	private LocalDate issueDate;

	private String reason;

	private BigDecimal taxRate;

	private BigDecimal subtotal;

	private BigDecimal taxAmount;

	private BigDecimal total;

	@ElementCollection
	@CollectionTable(name = "credit_note_line", joinColumns = @JoinColumn(name = "credit_note_id"))
	@OrderColumn(name = "position")
	private List<Invoice.Line> lines = new ArrayList<>();

	protected CreditNote() {
	}

	CreditNote(String number, Invoice invoice, LocalDate issueDate, String reason, List<Invoice.Line> lines,
			BigDecimal subtotal, BigDecimal taxAmount) {
		this.number = number;
		this.invoiceId = invoice.getId();
		this.invoiceNumber = invoice.getNumber();
		this.customerId = invoice.getCustomerId();
		this.issueDate = issueDate;
		this.reason = reason;
		this.taxRate = invoice.getTaxRate();
		this.lines.addAll(lines);
		this.subtotal = subtotal;
		this.taxAmount = taxAmount;
		this.total = subtotal.add(taxAmount);
	}

	public CreditNoteIssued issued() {
		return new CreditNoteIssued(getId(), number, invoiceNumber, customerId, issueDate, subtotal, taxAmount, total);
	}

}
