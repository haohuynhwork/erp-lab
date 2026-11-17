package com.erplab.purchasing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.erplab.common.domain.BaseEntity;
import com.erplab.common.domain.DomainException;
import com.erplab.common.domain.Quantities;
import com.erplab.purchasing.BillPaymentMade;

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

/**
 * The supplier's invoice for received goods, matched to the purchase order at order prices. The payable itself is
 * booked at goods receipt; the bill tracks what is due when and what was paid.
 */
@Getter
@Entity
@Table(name = "supplier_bill")
public class SupplierBill extends BaseEntity {

	public enum Status {
		OPEN, PARTIALLY_PAID, PAID
	}

	@Embeddable
	public record Line(Long productId, BigDecimal quantity, BigDecimal unitPrice) {

		BigDecimal amount() {
			return Quantities.money(quantity.multiply(unitPrice));
		}

	}

	private String number;

	/** The supplier's own invoice number. */
	private String supplierReference;

	private Long purchaseOrderId;

	private Long supplierId;

	private LocalDate billDate;

	private LocalDate dueDate;

	private BigDecimal total;

	private BigDecimal paidAmount = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	private Status status = Status.OPEN;

	@ElementCollection
	@CollectionTable(name = "supplier_bill_line", joinColumns = @JoinColumn(name = "bill_id"))
	@OrderColumn(name = "position")
	private List<Line> lines = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "bill_id", nullable = false)
	@OrderBy("id")
	private List<BillPayment> payments = new ArrayList<>();

	protected SupplierBill() {
	}

	SupplierBill(String number, String supplierReference, Long purchaseOrderId, Long supplierId, LocalDate billDate,
			LocalDate dueDate, List<Line> lines) {
		this.number = number;
		this.supplierReference = supplierReference;
		this.purchaseOrderId = purchaseOrderId;
		this.supplierId = supplierId;
		this.billDate = billDate;
		this.dueDate = dueDate;
		this.lines.addAll(lines);
		this.total = lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal balance() {
		return total.subtract(paidAmount);
	}

	public BillPaymentMade registerPayment(BigDecimal amount, LocalDate date, String method, String reference) {
		Quantities.requirePositive(amount, "amount");
		if (amount.compareTo(balance()) > 0) {
			throw new DomainException("overpayment", "Payment exceeds the open balance of " + balance());
		}
		payments.add(new BillPayment(date, amount, method, reference));
		paidAmount = paidAmount.add(amount);
		status = balance().signum() == 0 ? Status.PAID : Status.PARTIALLY_PAID;
		return new BillPaymentMade(getId(), number, supplierId, date, amount, method);
	}

}
