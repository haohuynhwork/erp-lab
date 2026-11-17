package com.erplab.sales.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.erplab.common.domain.BaseEntity;
import com.erplab.common.domain.DomainException;
import com.erplab.common.domain.Quantities;
import com.erplab.sales.InvoiceIssued;
import com.erplab.sales.InvoicePaymentReceived;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;

/** Customer invoice: Issued → Partially paid → Paid. Amounts never change after issue. */
@Getter
@Entity
@Table(name = "invoice")
public class Invoice extends BaseEntity {

	public enum Status {
		ISSUED, PARTIALLY_PAID, PAID, CREDITED
	}

	/** {@code creditedQuantity} is only used on invoice lines (zero on credit note lines). */
	@Embeddable
	public record Line(Long productId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal creditedQuantity) {

		Line(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
			this(productId, quantity, unitPrice, BigDecimal.ZERO);
		}

		BigDecimal amount() {
			return Quantities.money(quantity.multiply(unitPrice));
		}

	}

	private String number;

	private Long salesOrderId;

	private Long customerId;

	private LocalDate issueDate;

	private LocalDate dueDate;

	private BigDecimal taxRate;

	private BigDecimal subtotal;

	private BigDecimal taxAmount;

	private BigDecimal total;

	private BigDecimal paidAmount = BigDecimal.ZERO;

	private BigDecimal creditedAmount = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	private Status status = Status.ISSUED;

	@ElementCollection
	@CollectionTable(name = "invoice_line", joinColumns = @JoinColumn(name = "invoice_id"))
	@OrderColumn(name = "position")
	private List<Line> lines = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "invoice_id", nullable = false)
	@OrderBy("id")
	private List<InvoicePayment> payments = new ArrayList<>();

	protected Invoice() {
	}

	Invoice(String number, Long salesOrderId, Long customerId, LocalDate issueDate, LocalDate dueDate,
			BigDecimal taxRate, List<Line> lines) {
		this.number = number;
		this.salesOrderId = salesOrderId;
		this.customerId = customerId;
		this.issueDate = issueDate;
		this.dueDate = dueDate;
		this.taxRate = taxRate;
		this.lines.addAll(lines);
		this.subtotal = lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
		this.taxAmount = tax(subtotal, taxRate);
		this.total = subtotal.add(taxAmount);
	}

	static BigDecimal tax(BigDecimal subtotal, BigDecimal ratePercent) {
		return subtotal.multiply(ratePercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}

	public BigDecimal balance() {
		return total.subtract(paidAmount).subtract(creditedAmount);
	}

	private void updateStatus() {
		if (creditedAmount.compareTo(total) >= 0) {
			status = Status.CREDITED;
		}
		else if (balance().signum() == 0) {
			status = Status.PAID;
		}
		else {
			status = paidAmount.signum() > 0 ? Status.PARTIALLY_PAID : Status.ISSUED;
		}
	}

	/**
	 * Credits quantities of invoice lines; the credit can't exceed what is still unpaid.
	 * @param quantities line position (0-based) → quantity to credit
	 */
	public CreditNote credit(String creditNoteNumber, LocalDate date, String reason, Map<Integer, BigDecimal> quantities) {
		var credited = new ArrayList<Line>();
		var updated = new ArrayList<>(lines);
		for (var entry : quantities.entrySet()) {
			var qty = entry.getValue();
			if (qty == null || qty.signum() == 0) {
				continue;
			}
			Quantities.requirePositive(qty, "quantity");
			int pos = entry.getKey();
			if (pos < 0 || pos >= lines.size()) {
				throw new DomainException("unknown_line", "Invoice has no line " + (pos + 1));
			}
			var line = lines.get(pos);
			if (line.creditedQuantity().add(qty).compareTo(line.quantity()) > 0) {
				throw new DomainException("over_credit", "Cannot credit more than invoiced on line " + (pos + 1));
			}
			updated.set(pos, new Line(line.productId(), line.quantity(), line.unitPrice(), line.creditedQuantity().add(qty)));
			credited.add(new Line(line.productId(), qty, line.unitPrice()));
		}
		if (credited.isEmpty()) {
			throw new DomainException("nothing_credited", "Enter a quantity for at least one line");
		}
		var creditSubtotal = credited.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
		var creditTax = tax(creditSubtotal, taxRate);
		var creditTotal = creditSubtotal.add(creditTax);
		if (creditTotal.compareTo(balance()) > 0) {
			throw new DomainException("credit_exceeds_balance",
					"Credit of %s exceeds the unpaid balance of %s".formatted(creditTotal, balance()));
		}
		lines.clear();
		lines.addAll(updated);
		creditedAmount = creditedAmount.add(creditTotal);
		updateStatus();
		return new CreditNote(creditNoteNumber, this, date, reason, credited, creditSubtotal, creditTax);
	}

	public InvoiceIssued issued() {
		return new InvoiceIssued(getId(), number, customerId, issueDate, subtotal, taxAmount, total);
	}

	public InvoicePaymentReceived registerPayment(BigDecimal amount, LocalDate date, String method, String reference) {
		Quantities.requirePositive(amount, "amount");
		if (amount.compareTo(balance()) > 0) {
			throw new DomainException("overpayment", "Payment exceeds the open balance of " + balance());
		}
		payments.add(new InvoicePayment(date, amount, method, reference));
		paidAmount = paidAmount.add(amount);
		updateStatus();
		return new InvoicePaymentReceived(getId(), number, customerId, date, amount, method);
	}

}
